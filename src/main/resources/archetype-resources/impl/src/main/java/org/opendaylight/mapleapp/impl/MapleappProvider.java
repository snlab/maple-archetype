/*
 * Copyright © 2015 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mapleapp.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.maple.core.increment.MapleCore;
import org.opendaylight.maple.core.increment.app.MapleAppBase;
import org.opendaylight.maple.core.increment.packet.Ethernet;
import org.opendaylight.maple.core.increment.tracetree.*;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.maple.impl.ConfigFlowManager.FlowManagerWX;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MapleappProvider implements BindingAwareProvider, AutoCloseable, PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(MapleappProvider.class);

    private PacketProcessingService pps;

    private SalFlowService fs;

    private ListenerRegistration<NotificationListener> notificationListenerReg;

    MapleCore mapleCore;

    DataBroker db;

    //M3 mapleApp;


    MapleAppBase topMapleApp;

    FlowManagerWX fmwx;

    String chain;


    public MapleappProvider(String chain) {
        this.chain = chain;
    }


    @Override
    public void onSessionInitiated(ProviderContext session) {
        this.mapleCore = MapleCore.allocateMapleCore();
        if (chain.contains(",")){
            String[] apps = chain.split(",");
            for (int i = apps.length - 1; i >= 0; i--) {
                String name = apps[i];
                try {
                    MapleAppBase ap = (MapleAppBase) Class.forName(name).newInstance();
                    this.topMapleApp = ap;
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.out.println("maple app name: " + name);
            }
        } else {
            MapleAppBase ap = null;
            try {
                ap = (MapleAppBase) Class.forName("org.opendaylight.mapleapp.impl." + chain).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.topMapleApp = ap;
        }
        LOG.info("MapleappProvider Session Initiated");
        pps = session.getRpcService(PacketProcessingService.class);
        db = session.getSALService(DataBroker.class);
        fmwx = new FlowManagerWX(db, pps);
        if(this.mapleCore == null) {
            LOG.info("MapleCore is empty in External Mapleapp");
        }
        NotificationProviderService nps = session.getSALService(NotificationProviderService.class);
        this.notificationListenerReg = nps.registerNotificationListener(this);
    }

    @Override
    public void close() throws Exception {
        LOG.info("MapleappProvider Closed");
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        LOG.info("External Mapleapp onPacketRecieved is invorked");
        byte[] payload = packetReceived.getPayload();

        Ethernet frame = new Ethernet();

        frame.deserialize(payload, 0, payload.length);


        if (frame.getEtherType() == Ethernet.TYPE_ARP || frame.getEtherType() == Ethernet.TYPE_IPv4) {

            String tpIdString = fmwx.getTpIdFromNodeConnectorRef(packetReceived.getIngress()).getValue(); // "openFlow:1:1"

            System.out.println("tpIdString: " + tpIdString);

            Port port = new Port(tpIdString);

            MaplePacket maplePkt = new MaplePacket(frame, port);

            String pktHash = maplePkt.toString();

            System.out.println("hash: " + pktHash);

            this.mapleCore.getPktHash2PayLoadPort().put(pktHash
                    , new PayLoadAndPort(payload, new Port(tpIdString)));

            topMapleApp.preWork(maplePkt);

            topMapleApp.onPacket(maplePkt);

            Trace trace = new Trace();

            for (TraceItem ti : maplePkt.itemList) {
                trace.addTraceItem(ti, pktHash);
            }
            trace.addTraceItem(maplePkt.getInstruction().toItem(), pktHash);

            fmwx.sendPacket(payload, port, maplePkt.route());

            //LOG.info("lastNode in trace: " + trace.lastNode.toString());

            mapleCore.updateTrace(pktHash, trace);

            LOG.info("finish update trace");
        }
    }
}