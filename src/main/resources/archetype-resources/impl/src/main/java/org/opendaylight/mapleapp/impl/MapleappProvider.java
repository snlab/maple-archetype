/*
 * Copyright © 2015 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mapleapp.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.maple.core.increment.MapleCore;
import org.opendaylight.maple.core.increment.app.MapleAppBase;
import org.opendaylight.maple.core.increment.packet.Ethernet;
import org.opendaylight.maple.core.increment.packet.IPv4;
import org.opendaylight.maple.core.increment.tracetree.*;
import org.opendaylight.maple.impl.ConfigFlowManager.FlowManagerWX;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MapleappProvider implements BindingAwareProvider, AutoCloseable, PacketProcessingListener, DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(MapleappProvider.class);

    private PacketProcessingService pps;
    private SalFlowService fs;
    private ListenerRegistration<NotificationListener> notificationListenerReg;
    private MapleCore mapleCore;
    private DataBroker db;

    private MapleAppBase topMapleApp;
    private FlowManagerWX fmwx;

    private String chain;
    private boolean needSysApp;
    
    Map<MaplePacket, PayLoadAndPort> maplePktMap = new ConcurrentHashMap<MaplePacket, PayLoadAndPort>();
    
    public MapleappProvider(String chain, boolean needSysApp) {
        this.chain = chain;
        this.needSysApp = needSysApp;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        this.mapleCore = MapleCore.allocateMapleCore();
        if(this.mapleCore == null) {
            LOG.info("MapleCore is empty in External Mapleapp");
        }
        if (needSysApp) {
            this.mapleCore.setupSystemApps();
        }

            String[] apps = chain.split(",");
            for (int i = apps.length - 1; i >= 0; i--) {
                String name = apps[i];
                try {
                    MapleAppBase ap = (MapleAppBase) Class.forName("org.opendaylight.mapleapp.impl." + name).newInstance();
                    this.topMapleApp = ap;
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.out.println("maple app name: " + name);
            }

        LOG.info("MapleappProvider Session Initiated");
        pps = session.getRpcService(PacketProcessingService.class);
        db = session.getSALService(DataBroker.class);
        fmwx = new FlowManagerWX(db, pps);

        NotificationProviderService nps = session.getSALService(NotificationProviderService.class);
        this.notificationListenerReg = nps.registerNotificationListener(this);
    }

    @Override
    public void close() throws Exception {
        LOG.info("MapleappProvider Closed");
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {

        byte[] payload = packetReceived.getPayload();
        Ethernet frame = new Ethernet();
        frame.deserialize(payload, 0, payload.length);

        if (frame.getEtherType() == Ethernet.TYPE_ARP || frame.getEtherType() == Ethernet.TYPE_IPv4) {

            if (frame.getEtherType() == Ethernet.TYPE_IPv4) {
                IPv4 pIP = (IPv4) frame.getPayload();
                int srcIp = pIP.getSourceAddress();
                String srcIpString = IPv4.fromIPv4Address(srcIp);
                if (!srcIpString.startsWith("10"))return;
            }
            String packetType = null;
            if (frame.getEtherType() == Ethernet.TYPE_ARP) {
                packetType = "arp";
            } else if (frame.getEtherType() == Ethernet.TYPE_IPv4) {
                packetType = "ipv4";
            }
            double startTime = System.currentTimeMillis();
            String tpIdString = fmwx.getTpIdFromNodeConnectorRef(packetReceived.getIngress()).getValue(); // "openFlow:1:1"

            Port port = new Port(tpIdString);
            MaplePacket maplePkt = new MaplePacket(frame, port);
            String pktHash = maplePkt.toString();
            this.mapleCore.getPktHash2PayLoadPort().put(pktHash
                    , new PayLoadAndPort(payload, new Port(tpIdString)));

            topMapleApp.preWork(maplePkt);
            topMapleApp.onPacket(maplePkt);
            Trace trace = new Trace();
            List<TraceItem> compressedItemList = Trace.compress(maplePkt.itemList);

            for (TraceItem ti : compressedItemList) {
                trace.addTraceItem(ti, pktHash);
            }
            trace.addTraceItem(maplePkt.getInstruction().toItem(), pktHash);
            fmwx.sendPacket(payload, port, maplePkt.route());
            mapleCore.updateTrace(pktHash, trace);
            LOG.info("finish update trace");

            // start to handle data change for topology
            if (maplePkt.isReadTopo) {
                LOG.info("save topo reading packet");
                maplePktMap.put(maplePkt, new PayLoadAndPort(payload, new Port(tpIdString)));
            }
            
            double endTime = System.currentTimeMillis();
            LOG.info("use time: " + (endTime - startTime) + " for packet: " + packetType);
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

    }
}
