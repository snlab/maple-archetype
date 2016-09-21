/*
 * Copyright © 2016 Copyright (c) 2016 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.ConfigFlowManager;

import static org.maple.core.increment.tracetree.Match.Field.IPv4_SRC;
import static org.opendaylight.maple.impl.odlmaple.utils.FlowUtils.createFlow;
import static org.opendaylight.maple.impl.odlmaple.utils.FlowUtils.createModifiedFlow;
import static org.opendaylight.maple.impl.odlmaple.utils.FlowUtils.flowTableId;
import static org.opendaylight.maple.impl.odlmaple.utils.Maple2ODLUtils.setODLMatch;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.maple.core.increment.MapleDataPathAdaptor;
import org.maple.core.increment.packet.IPv4;
import org.maple.core.increment.tracetree.Drop;
import org.maple.core.increment.tracetree.Flood;
import org.maple.core.increment.tracetree.Path;
import org.maple.core.increment.tracetree.Port;
import org.maple.core.increment.tracetree.Punt;
import org.maple.core.increment.tracetree.RouteAction;
import org.maple.core.increment.tracetree.Rule;
import org.maple.core.increment.tracetree.SetPacketHeaderAction;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.maple.impl.odlmaple.utils.FlowUtils;
import org.opendaylight.maple.impl.odlmaple.utils.PacketUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class FlowManagerWX implements MapleDataPathAdaptor{
	
	private static final Logger LOG = LoggerFactory.getLogger(FlowManagerWX.class);
	
	private AtomicLong flowIdInc = new AtomicLong(10L);
	
	private AtomicLong flowCookieInc = new AtomicLong(0x3a00000000000000L);
	
	Map<FlowInfoKey, InstanceIdentifier<Flow>> flowInfo = new HashMap<>();
	
	private static final short DEFAULT_TABLE_ID = 0;
	
	private final Integer DEFAULT_HARD_TIMEOUT = 0;
    private final Integer DEFAULT_IDLE_TIMEOUT = 0;
    private final Long OFP_NO_BUFFER = Long.valueOf(4294967295L);
    
    private PacketProcessingService packetProcessingService;
    
    DataBroker dataBroker;
    
    public FlowManagerWX(DataBroker dataBroker, PacketProcessingService pps) {
    	this.dataBroker = dataBroker;
    	this.packetProcessingService = pps;
    }

	@Override
	public void deletePath(org.maple.core.increment.tracetree.Instruction inst, 
			org.maple.core.increment.tracetree.Match match) {
		Match odlMatch = this.generateODLMatch(match);
		RouteAction routeAction = inst.getRouteAction();
		
		if (routeAction instanceof Punt) {
			
		} else if (routeAction instanceof Drop) {
			
		} else if (routeAction instanceof Flood) {
			
		} else if (routeAction instanceof Path) {
			Path path = (Path)routeAction;
			
			for (String link: path.links) {
				String srcTpIdString = Path.getSrcTpId(link);
				TpId srcTpId = new TpId(srcTpIdString);
				NodeConnectorRef ncr = getNodeConnectorRefFromTpId(srcTpId);
				removeFlow(odlMatch, ncr);
			}
			String lastTpIdString = path.lastTpId;
			TpId lastTpId = new TpId(lastTpIdString);
			NodeConnectorRef ncr = getNodeConnectorRefFromTpId(lastTpId);
			removeFlow(odlMatch, ncr);
		}
	}
	
	private void removeFlow(Match match, NodeConnectorRef ncr) {
		TableKey flowTableKey = new TableKey(DEFAULT_TABLE_ID);
		
		InstanceIdentifier<Flow> flowPath = buildFlowPath(match, ncr, flowTableKey);
		
		LOG.info("Removed flow path: " + flowPath.toString());
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, flowPath);
        try {
            writeTransaction.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed: {}", e.toString());
        }
        LOG.info("Transaction succeeded");
        this.flowInfo.remove(new FlowInfoKey(InstanceIdentifierUtils.generateNodeInstanceIdentifier(ncr), match));
	}

	@Override
	public void deleteRule(Rule arg0, Port arg1) {
		// TODO Auto-generated method stub
		
	}
	
	public NodeConnectorRef getNodeConnectorRefFromTpId(TpId tpId){
		String nc_value = tpId.getValue();
		System.out.println("nc_value:"+nc_value);
		InstanceIdentifier<NodeConnector> ncid = InstanceIdentifier.builder(Nodes.class)
				.child(Node.class, new NodeKey(new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')))))
				.child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nc_value))).build();
		return new NodeConnectorRef(ncid);
	}

	public TpId getTpIdFromNodeConnectorRef(NodeConnectorRef ncf){
		NodeConnectorId nci = ncf.getValue()
	            .firstKeyOf(NodeConnector.class, NodeConnectorKey.class)
	            .getId();
		return new TpId(nci);
	}
	
	private static final Long IP_ETHER_TYPE = 0x0800L;
	
	private Match generateODLMatch(org.maple.core.increment.tracetree.Match m) {
		MatchBuilder matchBuilder = new MatchBuilder();
		EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
		Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
		MacAddress addr;
		for (Map.Entry<org.maple.core.increment.tracetree.Match.Field, String> entry: m.fields.entrySet()) {
			switch (entry.getKey()) {
			case ETH_SRC:
				addr = PacketUtils.macValueToMac(Long.valueOf((entry.getValue())));
				ethernetMatchBuilder.setEthernetSource(new EthernetSourceBuilder().setAddress(addr).build());
				break;
			case ETH_DST:
				addr = PacketUtils.macValueToMac(Long.valueOf(entry.getValue()));
				ethernetMatchBuilder.setEthernetDestination(new EthernetDestinationBuilder().setAddress(addr).build());
				break;
			case ETH_TYPE:
				ethernetMatchBuilder
				.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(Long.valueOf(entry.getValue()))).build());
				break;
			case IPv4_SRC:
				ipv4MatchBuilder.setIpv4Source(
						new Ipv4Prefix(IPv4.fromIPv4Address(Integer.parseInt(entry.getValue())) + "/32"));
                LOG.info("IPv4_SRC is :{}", IPv4.fromIPv4Address(Integer.parseInt(entry.getValue())));
                matchBuilder.setLayer3Match(ipv4MatchBuilder.build());
                ethernetMatchBuilder
				.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(IP_ETHER_TYPE)).build());
                break;
			case IPv4_DST:
				ipv4MatchBuilder.setIpv4Destination(
						new Ipv4Prefix(IPv4.fromIPv4Address(Integer.parseInt(entry.getValue())) + "/32"));
                LOG.info("IPv4_DST is :{}", IPv4.fromIPv4Address(Integer.parseInt(entry.getValue())));
                matchBuilder.setLayer3Match(ipv4MatchBuilder.build());
                ethernetMatchBuilder
				.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(IP_ETHER_TYPE)).build());
                break;
			default:
				break;
			}
		}
		matchBuilder.setEthernetMatch(ethernetMatchBuilder.build());
		Match result = matchBuilder.build();
		return result;
	}
	
	private InstanceIdentifier<Flow> buildFlowPath(Match match, NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {

        InstanceIdentifier<Node> nodeIID = InstanceIdentifierUtils.generateNodeInstanceIdentifier(nodeConnectorRef);
        FlowInfoKey flowInfoKey = new FlowInfoKey(nodeIID, match);
        InstanceIdentifier<Flow> flowPath;
        if (!this.flowInfo.containsKey(flowInfoKey)) {
            FlowKey flowKey = new FlowKey(new FlowId(String.valueOf(this.flowIdInc.getAndIncrement())));
            flowPath = InstanceIdentifierUtils.generateFlowInstanceIdentifier(nodeConnectorRef, flowTableKey, flowKey);
            this.flowInfo.put(flowInfoKey, flowPath);
        } else {
            LOG.info("You should be here");
            flowPath = this.flowInfo.get(flowInfoKey);
        }

        return flowPath;
    }
	
	private void writeFlow(Match match, NodeConnectorRef ncr, int priority) {
		TableKey flowTableKey = new TableKey(DEFAULT_TABLE_ID);

        InstanceIdentifier<Flow> flowPath = buildFlowPath(match, ncr, flowTableKey);
        FlowId flowId = flowPath.firstKeyOf(Flow.class).getId();

        LOG.info("flowPath: " + flowPath.toString());
        LOG.info("flowId: " + flowId.toString());


		Uri ncrUri = ncr.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
        Flow flowBody = createFlow(flowId, flowTableKey.getId(), priority, match, ncrUri);
        
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, flowPath, flowBody, true);
        try {
            writeTransaction.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed: {}", e.toString());
        }
        LOG.info("Transaction succeeded");
	}
	
	private Flow createToControllerFlow(FlowId flowId, Short tableId, int priority, 
			Match match) {
		Instructions instructions = this.createToControllerInsts();
		String flowName = flowId.toString();
		
		return createSpecFlow(flowId, flowName, tableId, priority, match, instructions);
	}
	
	private Flow createFlow(FlowId flowId, Short tableId, int priority, 
			Match match, Uri ncr) {
		Instructions instructions = createInstructions(ncr);
		String flowName = flowId.toString();
		
		return createSpecFlow(flowId, flowName, tableId, priority, match, instructions);
	}
	
	private Flow createSpecFlow(FlowId flowId, String flowName, Short tableId, int priority, Match match, Instructions instructions) {
        return new FlowBuilder()
                .setId(flowId)
                .setFlowName(flowName)
                .setTableId(tableId)
                .setMatch(match)
                .setInstructions(instructions)
                .setPriority(priority)
                .setBufferId(OFP_NO_BUFFER)
                .setHardTimeout(DEFAULT_HARD_TIMEOUT)
                .setIdleTimeout(DEFAULT_IDLE_TIMEOUT)
                .setFlags(new FlowModFlags(false,false,false,false,false))
                .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .build();
    }
	
	private Instructions createInstructions(Uri dstPortUri) {
        List<Instruction> instructionList = new LinkedList<>();
        //Uri dstPortUri = dstPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
        instructionList.add(createApplyActionsInstruction(dstPortUri, 0));
        return new InstructionsBuilder()
                .setInstruction(instructionList)
                .build();
    }
	
	private Action getSendToControllerAction() {
	      Action sendToController = new ActionBuilder()
	          .setOrder(0)
	          .setKey(new ActionKey(0))
	          .setAction(new OutputActionCaseBuilder()
	              .setOutputAction(new OutputActionBuilder()
	                  .setMaxLength(0xffff)
	                  .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
	                  .build())
	              .build())
	          .build();
	      return sendToController;
	    }
	
	// test toController
	private Instructions createToControllerInsts() {
		List<Action> actions = new ArrayList<Action>();
	      actions.add(getSendToControllerAction());

	      // Create an Apply Action
	      ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions)
	          .build();
	      
		Instruction applyActionsInstruction = new InstructionBuilder() //
		          .setOrder(0)
		          .setInstruction(new ApplyActionsCaseBuilder()//
		              .setApplyActions(applyActions) //
		              .build()) //
		          .build();
		
		Instructions insts = new InstructionsBuilder() //
	              .setInstruction(ImmutableList.of(applyActionsInstruction)) //
	              .build();
		
		return insts;
	}
	
	private Instruction createApplyActionsInstruction(Uri dstPortUri, int order) {
        Action outputToControllerAction = new ActionBuilder()
                .setOrder(0)
                .setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder()
                                .setMaxLength(0xffff)
                                .setOutputNodeConnector(dstPortUri)
                                .build())
                        .build())
                .build();
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
                .build();
        return new InstructionBuilder()
                .setOrder(order)
                .setInstruction(new ApplyActionsCaseBuilder()
                        .setApplyActions(applyActions)
                        .build())
                .build();
    }

	@Override
	public void installPath(org.maple.core.increment.tracetree.Instruction inst, 
			org.maple.core.increment.tracetree.Match match, 
			int priority) {
		Match odlMatch = generateODLMatch(match);
		RouteAction routeAction = inst.getRouteAction();
		
		if (routeAction instanceof Punt) {
			// not need 
		} else if (routeAction instanceof Drop) {
			// not need 
		} else if (routeAction instanceof Flood) {
			// not need
		} else if (routeAction instanceof Path) {
			Path path = (Path)routeAction;
			for (String link: path.links) {
				String srcTpIdString = Path.getSrcTpId(link);
				TpId srcTpId = new TpId(srcTpIdString);
				NodeConnectorRef ncr = getNodeConnectorRefFromTpId(srcTpId);
				writeFlow(odlMatch, ncr, priority);
			}
			String lastTpIdString = path.lastTpId;
			TpId lastTpId = new TpId(lastTpIdString);
			NodeConnectorRef ncr = getNodeConnectorRefFromTpId(lastTpId);
			
			if (inst.getSPHActions().isEmpty()) {
				writeFlow(odlMatch, ncr, priority);
			} else {
				// need modification
				Rule rule = new Rule(match, inst, "temphash");
				installRule(rule, new Port(lastTpIdString), priority);
			}
			
			
		}
	}

	@Override
	public void installRule(Rule rule, Port port, int priority) {
		// TODO Auto-generated method stub
		Match odlMatch = generateODLMatch(rule.match);
		org.maple.core.increment.tracetree.Instruction inst = rule.inst;
		TableKey flowTableKey = new TableKey(DEFAULT_TABLE_ID);
		RouteAction routeAction = inst.getRouteAction();
		TpId tpId = new TpId(port.toString());
		NodeConnectorRef nodeConnectorRef = getNodeConnectorRefFromTpId(tpId);
		InstanceIdentifier<Flow> flowPath = buildFlowPath(odlMatch , nodeConnectorRef, flowTableKey);
		FlowId flowId = flowPath.firstKeyOf(Flow.class).getId();
		Flow flowBody = null;
		if (routeAction instanceof Flood) {
			/*Uri flood = new Uri(OutputPortValues.FLOOD.toString());
			flowBody = createFlow(flowId, DEFAULT_TABLE_ID, priority, odlMatch, flood);
			LOG.info("maple installRule Flood flowBody is :{}", flowBody);*/
			return;
		} else if (routeAction instanceof Drop) {
			return;
		} else if (routeAction instanceof Punt) {
			Uri controllerPort = new Uri(OutputPortValues.CONTROLLER.toString());
			//flowBody = createFlow(flowId, DEFAULT_TABLE_ID, priority, odlMatch, controllerPort);
			flowBody = this.createToControllerFlow(flowId, DEFAULT_TABLE_ID, priority, odlMatch);
			LOG.info("maple installRule Punt flowPath is :{}", flowPath);
			LOG.info("maple installRule Punt flowBody is :{}", flowBody);
		} else {
			// path means modification
			Uri destPortUri = nodeConnectorRef.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
			flowBody = createModifiedFlow(odlMatch, priority, destPortUri, inst.getSPHActions(), flowId);
			LOG.info("maple installRule modified flowBody is :{}", flowBody);
		}
		WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
		writeTransaction.put(LogicalDatastoreType.CONFIGURATION, flowPath, flowBody, true);
		try {
			writeTransaction.submit().checkedGet();
		} catch (TransactionCommitFailedException e) {
			LOG.error("Transaction failed: {}", e.toString());
		}
		LOG.info("Transaction succeeded");
	}

	@Override
	public void sendPacket(byte[] payLoad, Port port, 
			org.maple.core.increment.tracetree.Action action) {
		// TODO Auto-generated method stub
		if (action instanceof Flood) {
			// port means ingressPort
			String tpIdString = port.getId();
			TpId tpId = new TpId(tpIdString);
			sendFloodPacket(payLoad, this.getNodeConnectorRefFromTpId(tpId));
		} else {
			// port means outPort
		}
	}
	
	private void sendFloodPacket(byte[] payload, NodeConnectorRef ingress) {
        LOG.info("Maple sendFloodPacket is invoked");
        /* Read node of ingress */
        String nodeId = ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class)
                .getId().getValue();
        InstanceIdentifier<Node> nodeIId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId(nodeId))).build();
        com.google.common.base.Optional<Node > node = null;
        try {
            ReadOnlyTransaction rx = this.dataBroker.newReadOnlyTransaction();
            node = rx.read(LogicalDatastoreType.OPERATIONAL, nodeIId).get();
        } catch (InterruptedException e) {
            LOG.error("Fast read DataStore failed");
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        /* Transmit packet-out when node connector is not the ingress */
        List<NodeConnector> ncList = node.get().getNodeConnector();
        for (NodeConnector nc : ncList) {
            NodeConnectorId ncId = nc.getId();
            InstanceIdentifier<NodeConnector> ncII = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, new NodeKey(new NodeId(nodeId)))
                    .child(NodeConnector.class, new NodeConnectorKey(ncId)).toInstance();
            NodeConnectorRef ncf = new NodeConnectorRef(ncII);

            String ncIdString = ncf.getValue().firstIdentifierOf(NodeConnector.class)
                    .firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue();
            if (!ncIdString.equals(ingress.getValue().firstIdentifierOf(NodeConnector.class)
                    .firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue())) {
                TransmitPacketInput input = FlowUtils.createPacketOut(payload, ingress, ncf);
                packetProcessingService.transmitPacket(input);
            }
        }
    }

}
