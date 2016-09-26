/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mapleapp.impl;

import org.opendaylight.maple.core.increment.tracetree.MaplePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ${appName} extends MapleAppBase {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(${appName}.class);
	ShortestPath sp;

	@Override
	public void onPacket(MaplePacket pkt) {
		LOG.info("Hello world from MapleApp.");
		/* write your Maple App here */
	}
}
