/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.odlmaple.utils;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static org.maple.core.increment.tracetree.Match.Field.*;
import static org.opendaylight.maple.impl.odlmaple.utils.PacketUtils.macValueToMac;

public class Maple2ODLUtils {
    public Maple2ODLUtils() {
    }
    private static final Logger LOG = LoggerFactory.getLogger(Maple2ODLUtils.class);
    /**
     * Convert maple packet match field to odl match field
     *
     * @param mapleMatch
     * @return ODL Match field
     */
    public static Match setODLMatch (org.maple.core.increment.tracetree.Match mapleMatch) {
        MatchBuilder matchBuilder = new MatchBuilder();
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
        Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
        MacAddress macAddr;
        Set<org.maple.core.increment.tracetree.Match.Field> matches = mapleMatch.fields.keySet();
        for(org.maple.core.increment.tracetree.Match.Field match : matches){
            try {
                if (match.equals(ETH_SRC)) {
                    macAddr = macValueToMac(Long.valueOf(mapleMatch.fields.get(ETH_SRC)));
                    LOG.info("SRCmac is :{}", macAddr);
                    ethernetMatchBuilder.setEthernetSource(new EthernetSourceBuilder().setAddress(macAddr).build());
                    matchBuilder.setEthernetMatch(ethernetMatchBuilder.build());
                }  else if (match.equals(ETH_DST)) {
                    macAddr = macValueToMac(Long.valueOf(mapleMatch.fields.get(ETH_DST)));
                    LOG.info("DSTmac is :{}", macAddr);
                    ethernetMatchBuilder.setEthernetDestination(new EthernetDestinationBuilder().setAddress(macAddr).build());
                    matchBuilder.setEthernetMatch(ethernetMatchBuilder.build());
                } /*else if (match.equals(ETH_TYPE)) {
                    ethernetMatchBuilder.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(Long.valueOf(mapleMatch.fields.get(ETH_TYPE)))).build());
                    LOG.info("ETH_TYPE is:{}",mapleMatch.fields.get(ETH_TYPE));
                    matchBuilder.setEthernetMatch(ethernetMatchBuilder.build());
                }*/ else if (match.equals(IPv4_SRC)) {
                    ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(mapleMatch.fields.get(IPv4_SRC)));
                    LOG.info("IPv4_SRC is :{}", mapleMatch.fields.get(IPv4_SRC));
                    matchBuilder.setLayer3Match(ipv4MatchBuilder.build());
                } else if (match.equals(IPv4_DST)) {
                    ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(mapleMatch.fields.get(IPv4_DST)));
                    LOG.info("IPv4_DST is :{}", mapleMatch.fields.get(IPv4_DST));
                    matchBuilder.setLayer3Match(ipv4MatchBuilder.build());
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        Match result = matchBuilder.build();
        return result;
    }
}
