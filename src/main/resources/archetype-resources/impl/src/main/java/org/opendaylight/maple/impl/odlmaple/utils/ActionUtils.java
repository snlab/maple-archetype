/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.odlmaple.utils;

import org.maple.core.increment.packet.IPv4;
import org.maple.core.increment.tracetree.Match;
import org.maple.core.increment.tracetree.SetPacketHeaderAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import java.util.ArrayList;
import java.util.List;
import static org.opendaylight.maple.impl.odlmaple.utils.PacketUtils.macValueToMac;

public class ActionUtils {

    protected static List<Action> createModifiedActions(Uri destPortUri, List<SetPacketHeaderAction> modi_field) {
        List<Action> actionList = new ArrayList<Action>();
        int i = 0;
        //Construct a output action


        //Construct modify action(s)
        if (!modi_field.isEmpty()) {
            for (SetPacketHeaderAction setPacketHeaderAction : modi_field) {
                if (setPacketHeaderAction.getField().equals(Match.Field.ETH_SRC)) {
                    String target = setPacketHeaderAction.getValue();
                    MacAddress macAddress = macValueToMac(Long.parseLong(target));
                    Action modifySrcMacAction = modifySrcMac(macAddress, i++);
                    actionList.add(modifySrcMacAction);
                } else if (setPacketHeaderAction.getField().equals(Match.Field.ETH_DST)) {
                    String target = setPacketHeaderAction.getValue();
                    MacAddress macAddress = macValueToMac(Long.parseLong(target));
                    Action modifyDstMacAction = modifyDstMac(macAddress, i++);
                    actionList.add(modifyDstMacAction);
                } else if (setPacketHeaderAction.getField().equals(Match.Field.IPv4_SRC)) {
                    String target = setPacketHeaderAction.getValue();
                    Ipv4Prefix ipAddress = new Ipv4Prefix(IPv4.fromIPv4Address(Integer.parseInt(target)) + "/32");
                    Action modifySrcIpAction = modifySrcIp(ipAddress, i++);
                    actionList.add(modifySrcIpAction);
                } else if (setPacketHeaderAction.getField().equals(Match.Field.IPv4_DST)) {
                    String target = setPacketHeaderAction.getValue();
                    Ipv4Prefix ipAddress = new Ipv4Prefix(IPv4.fromIPv4Address(Integer.parseInt(target)) + "/32");
                    Action modifyDstIpAction = modifyDstIp(ipAddress, i++);
                    actionList.add(modifyDstIpAction);
                }
            }
        }

        OutputAction outputAction = new OutputActionBuilder()
                .setMaxLength(0xffff)
                .setOutputNodeConnector(destPortUri)
                .build();
        Action outputToControllerAction = new ActionBuilder()
                .setOrder(i++)
                .setAction(new OutputActionCaseBuilder().setOutputAction(outputAction).build())
                .build();
        actionList.add(outputToControllerAction);

        return actionList;
    }

    /**
     * Construct a destination Ip address modification action
     * @param ipPrefix
     * @return Action
     */
    private static Action modifyDstIp(Ipv4Prefix ipPrefix, int i) {
        Ipv4Builder ipv4Builder = new Ipv4Builder();
        ipv4Builder.setIpv4Address(ipPrefix);
        SetNwDstActionBuilder setNwDstActionBuilder = new SetNwDstActionBuilder();
        setNwDstActionBuilder.setAddress(ipv4Builder.build());
        SetNwDstAction setNwDstAction = setNwDstActionBuilder.build();
        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setOrder(i);
        actionBuilder.setAction(new SetNwDstActionCaseBuilder().setSetNwDstAction(setNwDstAction).build());
        return actionBuilder.build();
    }

    /**
     * Construct a source Ip address modification action
     * @param ipPrefix
     * @return Action
     */
    private static Action modifySrcIp(Ipv4Prefix ipPrefix, int i) {
        Ipv4Builder ipv4Builder = new Ipv4Builder();
        ipv4Builder.setIpv4Address(ipPrefix);
        SetNwSrcActionBuilder setNwSrcActionBuilder = new SetNwSrcActionBuilder();
        setNwSrcActionBuilder.setAddress(ipv4Builder.build());
        SetNwSrcAction setNwSrcAction = setNwSrcActionBuilder.build();
        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setOrder(i);
        actionBuilder.setAction(new SetNwSrcActionCaseBuilder().setSetNwSrcAction(setNwSrcAction).build());
        return actionBuilder.build();
    }

    /**
     * Construct a destination mac address modification action
     * @param macAddress
     * @return Action
     */
    private static Action modifyDstMac(MacAddress macAddress, int i) {
        SetDlDstActionBuilder setDstdl = new SetDlDstActionBuilder();
        setDstdl.setAddress(macAddress);
        SetDlDstAction setdlDstAction = setDstdl.build();
        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setOrder(i);
        actionBuilder.setAction(new SetDlDstActionCaseBuilder().setSetDlDstAction(setdlDstAction).build());
        return actionBuilder.build();
    }

    /**
     * Construct a source mac address modification action
     * @param macAddress
     * @return Action
     */
    private static Action modifySrcMac(MacAddress macAddress, int i) {
        SetDlSrcActionBuilder setSrcdl = new SetDlSrcActionBuilder();
        setSrcdl.setAddress(macAddress);
        SetDlSrcAction setdlSrcAction = setSrcdl.build();
        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setOrder(i);
        actionBuilder.setAction(new SetDlSrcActionCaseBuilder().setSetDlSrcAction(setdlSrcAction).build());
        return actionBuilder.build();
    }

}
