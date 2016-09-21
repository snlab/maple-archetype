/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl;

import org.maple.core.increment.MapleCore;
import org.maple.core.increment.app.M1;
import org.maple.core.increment.app.M2;
import org.maple.core.increment.app.M3;
import org.maple.core.increment.packet.Ethernet;
import org.maple.core.increment.tracetree.MaplePacket;
import org.maple.core.increment.tracetree.PayLoadAndPort;
import org.maple.core.increment.tracetree.Port;
import org.maple.core.increment.tracetree.Trace;
import org.maple.core.increment.tracetree.TraceItem;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.maple.impl.ConfigFlowManager.DataImpl;
import org.opendaylight.maple.impl.ConfigFlowManager.FlowManagerWX;
import org.opendaylight.maple.impl.app.MyMapleApp;
import org.opendaylight.maple.impl.odlmaple.data.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ODLControllerAdapter implements DataChangeListener, PacketProcessingListener {

	protected static final Logger LOG = LoggerFactory.getLogger(ODLControllerAdapter.class);

	private PacketProcessingService pps;
	
	private SalFlowService fs;

	DataBroker db;
	
	MapleCore mapleCore;
	
	//M3 mapleApp;

	MyMapleApp myMapleApp;

	FlowManagerWX fmwx;
	DataImpl dm;

	public ODLControllerAdapter(PacketProcessingService pps, SalFlowService fs, DataBroker dataBroker) {
		this.pps = pps;
		this.fs = fs;
		this.db = dataBroker;
		
		this.mapleCore = new MapleCore();
		this.myMapleApp = new MyMapleApp();
		
		fmwx = new FlowManagerWX(db, pps);
		dm = new DataImpl(db);
		
		//FlowManager fm = new FlowManager(db, pps);
		LOG.info("Maple FlowManager is initialized");
        
        //InventoryReader ir = new InventoryReader(db);
		LOG.info("Maple InventoryReader is initialized");
        
        this.mapleCore.setAdaptor(fmwx, dm);
	}

	@Override
	public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

	}

	@Override
	public void onPacketReceived(PacketReceived packet) {
		byte[] payload = packet.getPayload();
		
		Ethernet frame = new Ethernet();
    	
        frame.deserialize(payload, 0, payload.length);



        if (frame.getEtherType() == Ethernet.TYPE_ARP || frame.getEtherType() == Ethernet.TYPE_IPv4) {
        	
        	String tpIdString = fmwx.getTpIdFromNodeConnectorRef(packet.getIngress()).getValue(); // "openFlow:1:1"
        	
        	System.out.println("tpIdString: " + tpIdString);
        	
        	Port port = new Port(tpIdString);
        	
        	MaplePacket maplePkt = new MaplePacket(frame, port);
            
            String pktHash = maplePkt.toString();
            
            System.out.println("hash: " + pktHash);
            
            this.mapleCore.getPktHash2PayLoadPort().put(pktHash
            		, new PayLoadAndPort(payload, new Port(tpIdString)));
            
            
            myMapleApp.onPacket(maplePkt);
            
            Trace trace = new Trace();
            
            for (TraceItem ti: maplePkt.itemList) {
            	trace.addTraceItem(ti, pktHash);
            }
            trace.addTraceItem(maplePkt.getInstruction().toItem(), pktHash);
            
            fmwx.sendPacket(payload, port, maplePkt.getRouteAction());
            
            //LOG.info("lastNode in trace: " + trace.lastNode.toString());
            
            mapleCore.updateTrace(pktHash, trace);
            
            LOG.info("finish update trace");
        }
	}

}
