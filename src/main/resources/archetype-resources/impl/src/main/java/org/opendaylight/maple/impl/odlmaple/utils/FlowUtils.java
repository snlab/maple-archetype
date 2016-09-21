/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.odlmaple.utils;

import com.google.common.collect.ImmutableList;
import org.maple.core.increment.tracetree.SetPacketHeaderAction;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.opendaylight.maple.impl.odlmaple.utils.ActionUtils.createModifiedActions;
import static org.opendaylight.maple.impl.odlmaple.utils.InstanceIdentifierUtils.createFlowIId;

public class FlowUtils {
    private static final Logger LOG = LoggerFactory.getLogger(FlowUtils.class);

    public static Short flowTableId;
    public int flowPriority;
    public static int flowIdleTimeout;
    public static int flowHardTimeout;
    public static AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);
    public static AtomicLong flowIdInc = new AtomicLong();

    private static final Short DEFAULT_TABLE_ID = 0;
    private final Integer DEFAULT_PRIORITY = 20;
    private final Integer DEFAULT_HARD_TIMEOUT = 3600;
    private final Integer DEFAULT_IDLE_TIMEOUT = 1800;
    private static final Long OFP_NO_BUFFER = Long.valueOf(4294967295L);

    public FlowUtils() {
        setFlowTableId(DEFAULT_TABLE_ID);
        setFlowPriority(DEFAULT_PRIORITY);
        setFlowIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        setFlowHardTimeout(DEFAULT_HARD_TIMEOUT);
    }

    public void setFlowTableId(Short flowTableId) {
        this.flowTableId = flowTableId;
    }

    public void setFlowPriority(Integer flowPriority) {
        this.flowPriority = flowPriority;
    }

    public void setFlowIdleTimeout(Integer flowIdleTimeout) {
        this.flowIdleTimeout = flowIdleTimeout;
    }

    public void setFlowHardTimeout(Integer flowHardTimeout) {
        this.flowHardTimeout = flowHardTimeout;
    }


    /**
     * Create punt flow body
     *
     * @param tableId
     * @param priority
     * @return {@link FlowBuilder} forwarding all packets to controller port
     */
    public static Flow createFlow(Short tableId, int priority, Match match, Uri outPutNodeConnector, FlowId flowId) {
        FlowBuilder flowBuilder = new FlowBuilder()
                .setTableId(tableId)
                .setFlowName("puntall");
        //FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
        flowBuilder.setId(flowId);

        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(Integer.valueOf(0xffff));
        output.setOutputNodeConnector(outPutNodeConnector);

        Action action = new ActionBuilder()
                .setOrder(0)
                .setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build())
                .build();

        flowInstrutionBuilder(flowBuilder, ImmutableList.of(action), match, priority);
        return flowBuilder.build();
    }

    /**
     *Create multicast flow body for one node
     *
     * @param match
     * @param ncrList
     * @param priority
     * @return multicast flow
     */
    public static Flow createMulticastFlow(Match match, List<NodeConnectorRef> ncrList, int priority, FlowId flowId) {
        FlowBuilder multiFlow = new FlowBuilder().setTableId(DEFAULT_TABLE_ID).setFlowName("MultiFlow");
        //FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
        multiFlow.setId(flowId);
        List<Action> actionList = new ArrayList<Action>();

        for (int i = 0; i < ncrList.size(); i++) {
            NodeConnectorRef ncr = ncrList.get(i);
            Uri destPortUri = ncr.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

            OutputAction outputAction = new OutputActionBuilder()
                    .setMaxLength(0xffff)
                    .setOutputNodeConnector(destPortUri)
                    .build();

            Action action = new ActionBuilder()
                    .setOrder(i)
                    .setAction(new OutputActionCaseBuilder().setOutputAction(outputAction).build())
                    .build();

            actionList.add(action);
        }
        flowInstrutionBuilder(multiFlow, actionList, match, priority);
        LOG.info("Maple multiFlow is:{}", multiFlow);
        return multiFlow.build();
    }

    /**
     * Create modified flow body by match and priority
     *
     * @param match
     * @param priority
     * @param destPortUri
     * @return flow body
     */
    public static Flow createModifiedFlow(Match match, int priority, Uri destPortUri,
                                   Map<org.maple.core.increment.tracetree.Match.Field, SetPacketHeaderAction> modi_field, FlowId flowId) {

        FlowBuilder modifiedFlow = new FlowBuilder()
                .setTableId(DEFAULT_TABLE_ID)
                .setFlowName("mac2mac");
        //FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
        modifiedFlow.setId(flowId);
        List<SetPacketHeaderAction> setPacketHeaderActions = new ArrayList<>();
        
        for (Map.Entry<org.maple.core.increment.tracetree.Match.Field, SetPacketHeaderAction> entry: modi_field.entrySet()) {
        	setPacketHeaderActions.add(entry.getValue());
        }

        List<Action> actions = createModifiedActions(destPortUri, setPacketHeaderActions);
        flowInstrutionBuilder(modifiedFlow, actions, match, priority);
        return modifiedFlow.build();
    }

    /**
     * Construct instruction field of Flow
     *
     * @param flowBuilder
     * @param actions
     * @param match
     * @param priority
     * @return
     */
    private static FlowBuilder flowInstrutionBuilder(FlowBuilder flowBuilder, List<Action> actions, Match match, int priority) {
        ApplyActions applyActions = new ApplyActionsBuilder()
                .setAction(actions)
                .build();

        Instruction instruction = new InstructionBuilder()
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()
                        .setApplyActions(applyActions)
                        .build())
                .build();
        flowBuilder.setMatch(match)
                .setInstructions(new InstructionsBuilder().setInstruction(ImmutableList.of(instruction)).build())
                .setPriority(priority)
                .setBufferId(OFP_NO_BUFFER)
                .setHardTimeout(flowHardTimeout)
                .setIdleTimeout(flowIdleTimeout)
                .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));
        return flowBuilder;
    }

    /**
     * Get termination point Id of "network inventory" from node connector reference of "inventory"
     *
      * @param ncf
     * @return termination point Id of "network inventory"
     */
    protected static TpId getTpIdFromNodeConnectorRef(NodeConnectorRef ncf) {
        NodeConnectorId nci = ncf.getValue()
                .firstKeyOf(NodeConnector.class, NodeConnectorKey.class)
                .getId();
        return new TpId(nci);
    }

    /**
     * Get node Id string from node connector reference
     * @param ncr
     * @return node id string
     */
    public static String getNodeString(NodeConnectorRef ncr) {
        String nodeIdString = ncr.getValue().firstIdentifierOf(Node.class)
                .firstKeyOf(Node.class, NodeKey.class).getId().getValue();
        return nodeIdString;
    }

    /**
     * Get node connector reference of "inventory" from termination Id of "network inventory"
     *
     * @param tpId
     * @return node connector ref
     */
    public static NodeConnectorRef getNodeConnectorRefFromTpId(TpId tpId){
        String nc_value = tpId.getValue();
        System.out.println("nc_value:"+nc_value);// Comments(Haizhou): suggest change to log.info();
        InstanceIdentifier<NodeConnector> ncid = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')))))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nc_value))).build();
        return new NodeConnectorRef(ncid); //??? Why do it return a new NodeConnectorRef()?
    }

    /**
     * Create AddFlowInput of multicast paths for one node for SalFlowService
     *
     * @param ncrList
     * @return AddFlowInput of SalFlowService
     */
    public static InstanceIdentifier<Flow> setMulticastPathForOneNode(List<NodeConnectorRef> ncrList, FlowId flowId){
        TableKey flowTableKey = new TableKey(DEFAULT_TABLE_ID);
        LOG.info("Maple TableKey is :{}", flowTableKey);
        //FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
        LOG.info("Maple flow id is :{}",flowId);
        InstanceIdentifier<Flow> flowIId = createFlowIId(ncrList.get(0), flowTableKey, flowId);
        LOG.info("Maple flowIId is :{}", flowIId);
        return flowIId;
    }

    /**
     * Create AddFlowInput by flow instance identifier and flow body for SalFlowService
     *
     * @param flowPath
     * @param flow
     * @return AddFlowInput of SalFlowService
     */
    @Deprecated
    public static AddFlowInput buildFlowInput(InstanceIdentifier<Flow> flowPath, Flow flow) {
        final InstanceIdentifier<Table> tableIId = flowPath.<Table>firstIdentifierOf(Table.class);
        final InstanceIdentifier<Node> nodeIId = flowPath.<Node>firstIdentifierOf(Node.class);
        final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
        builder.setNode(new NodeRef(nodeIId));
        builder.setFlowRef(new FlowRef(flowPath));
        builder.setFlowTable(new FlowTableRef(tableIId));
        builder.setTransactionUri(new Uri(flow.getId().getValue()));
        return builder.build();
    }

    /**
     * Get node connector reference by termination point
     *
     * @param ncr
     * @return Node connector reference
     */
    public static NodeConnectorRef toCtrlRef(NodeConnectorRef ncr) {
        TpId tpId = getTpIdFromNodeConnectorRef(ncr);
        NodeId nodeId = convertTpId2NodeId(tpId);
        String nodeIdString = nodeId.getValue();
        String toCtrRefString = nodeIdString+":CONTROLLER";
        NodeConnectorRef toCtrRef = getNodeConnectorRefFromTpId(new TpId(toCtrRefString));
        return toCtrRef;
    }

    /**
     * Get node Id by termination point
     *
     * @param tpId
     * @return Node Id
     */
    public static NodeId convertTpId2NodeId(TpId tpId) {
        String nc_value = tpId.getValue();
        return new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')));
    }

    /**
     * Create PacketOut body by payload, ingress, egress
     *
     * @param payload
     * @param ingress
     * @param egress
     * @return PacketOut body for PacketProcessingService
     */
    public static TransmitPacketInput createPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
        InstanceIdentifier<Node> egressNodePath = InstanceIdentifierUtils.getNodeIId(egress.getValue());
        TransmitPacketInput input = new TransmitPacketInputBuilder().setPayload(payload)
                .setNode(new NodeRef(egressNodePath)).setEgress(egress).setIngress(ingress).build();
        return input;
    }

    /**
     * Get NodeRef from NodeConnectorRef
     *
     * @param nodeConnectorRef
     * @return NodeRef
     */
    public static NodeRef getNodeRef(NodeConnectorRef nodeConnectorRef) {
        InstanceIdentifier<Node> nodeIID = nodeConnectorRef.getValue().firstIdentifierOf(Node.class);
        return new NodeRef(nodeIID);
    }
}