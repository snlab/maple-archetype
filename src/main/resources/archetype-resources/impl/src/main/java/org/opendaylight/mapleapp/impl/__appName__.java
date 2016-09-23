/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mapleapp.impl;

import org.opendaylight.maple.core.increment.app.MapleAppBase;
import org.opendaylight.maple.core.increment.app.ShortestPath;
import org.opendaylight.maple.core.increment.packet.Ethernet;
import org.opendaylight.maple.core.increment.packet.IPv4;
import org.opendaylight.maple.core.increment.tracetree.MaplePacket;
import org.opendaylight.maple.core.increment.tracetree.Path;
import org.opendaylight.maple.core.increment.tracetree.Port;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ${appName} extends MapleAppBase {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger( ${appName}.class);
	ShortestPath sp;
	private static class Singleton {
		private static  ${appName} instance = new  ${appName}();
	}
	public static ${appName} getInstance() {
		return Singleton.instance;
	}

	@Override
	public void onPacket(MaplePacket pkt) {
		LOG.info("External MapleApp is running");
		if (pkt.ethTypeIs(Ethernet.TYPE_IPv4)) {
			int srcIP = pkt.IPv4Src();
			int dstIP = pkt.IPv4Dst();
			
			for (Map.Entry<Integer, Port> entry: this.mapleCore.getHost2swTable().entrySet()) {
				System.out.println("hostIP: " + IPv4.fromIPv4Address(entry.getKey()));
				System.out.println("Port: " + entry.getValue().getId());
			}
			
			Topology topo = (Topology)readData("/root/network-topology/topology");
			sp = new ShortestPath();
			sp.setLinks(topo.getLink());
			
			Port srcPort = this.mapleCore.getHost2swTable().get(srcIP);
			Port dstPort = this.mapleCore.getHost2swTable().get(dstIP);
			
			
			String lastTpId = dstPort.getId();
			System.out.println("it is running external maple app");
			pkt.setRouteAction(new Path(sp.getFormattedPath(srcPort, dstPort), lastTpId));
		} else {
			//make the next maple app handle this pkt
			this.passToNext(pkt);
		}
	}
}
