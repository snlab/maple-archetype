/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.odlmaple.utils;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InstanceIdentifierUtils {

    private InstanceIdentifierUtils() {
    }

    /**
     * Create an Instance Identifier (path) for node with specified id
     *
     * @param nodeId
     * @return nodeIId
     */
    public static final InstanceIdentifier<Node> setNodeIId(NodeId nodeId) {

        return InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nodeId)).build();
    }

    /**
     * Set a Node iid of "inventory" by node of "network.topology.rev131021.NodeId"
     *
     * @param nodeId
     * @return nodeIId
     */

    public static final InstanceIdentifier<Node> setNodeIId(org.opendaylight.yang.gen.v1.urn.tbd
                                                                    .params.xml.ns.yang.network.topology
                                                                    .rev131021.NodeId nodeId) {

        return InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId(nodeId))).build();
    }

    /**
     * Shorten's node child path to node iid.
     *
     * @param nodeChild
     * @return nodeIId
     */
    protected static final InstanceIdentifier<Node> getNodeIId (InstanceIdentifier<?> nodeChild) {

        return nodeChild.firstIdentifierOf(Node.class);
    }

    /**
     * Create a table path by appending table specific location to node iid
     *
     * @param nodeIId
     * @param tableKey
     * @return tableIId
     */
    public static final InstanceIdentifier<Table> setTableIId(InstanceIdentifier<Node> nodeIId, TableKey tableKey) {

        return InstanceIdentifier.builder(nodeIId)
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableKey))
                .build();
    }

    /**
     * Create a path for particular odlmaple.flow, by appending odlmaple.flow-specific information
     * to table path.
     *
     * @param flowId
     * @param tableIId
     * @return flowIId
     */
    protected static InstanceIdentifier<Flow> setFlowIId(InstanceIdentifier<Table> tableIId, FlowId flowId) {

        return InstanceIdentifier.builder(tableIId)
                .child(Flow.class, new FlowKey(flowId))
                .build();
    }

    /**
     * Extract table id from table iid.
     *
     * @param tableIId
     * @return tableId
     */
    protected static Short getTableId(InstanceIdentifier<Table> tableIId) {

        return tableIId.firstKeyOf(Table.class, TableKey.class).getId();
    }

    /**
     * Extract NodeConnectorKey from NodeConnector iid
     *
     * @param nodeConnectorIId
     * @return nodeConnectorKey
     */
    protected static NodeConnectorKey getNodeConnectorKey(InstanceIdentifier<?> nodeConnectorIId) {

        return nodeConnectorIId.firstKeyOf(NodeConnector.class, NodeConnectorKey.class);
    }

    /**
     * Create node connector iid by node iid and node connector key
     *
     * @param nodeIId
     * @param nodeConnectorId
     * @return nodeConnectorIId
     */
    protected static final InstanceIdentifier<NodeConnector> setNodeConnectorIId(InstanceIdentifier<Node> nodeIId, NodeConnectorId nodeConnectorId) {

        return InstanceIdentifier.builder(nodeIId)
                .child(NodeConnector.class,new NodeConnectorKey(nodeConnectorId))
                .build();
    }

    /**
     * Set NodeConnectorIId by NodeId and NodeConnectorId
     *
     * @param nodeId
     * @param nodeConnectorId
     * @return NodeConnectorIId
     */
    protected static final InstanceIdentifier<NodeConnector> setNodeConnectorIId(NodeId nodeId, NodeConnectorId nodeConnectorId) {

        return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId))
                .build();
    }

    /**
     * Set FlowIId by NodeIId, TableKey, FlowId
     *
     * @param nodeIId
     * @param tableKey
     * @param flowId
     * @return FlowIId
     */
    public static final InstanceIdentifier<Flow> setFlowIId(InstanceIdentifier<Node> nodeIId, TableKey tableKey, FlowId flowId){

        return nodeIId.augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableKey))
                .child(Flow.class, new FlowKey(flowId));
    }

    /**
     * Extract node iid by node connector ref
     *
     * @param nodeConnectorRef
     * @return NodeIId
     */
    public static InstanceIdentifier<Node> getNodeIId(final NodeConnectorRef nodeConnectorRef) {

        return nodeConnectorRef.getValue().firstIdentifierOf(Node.class);
    }

    /**
     * Create Table iid by node connector ref and table key
     *
     * @param nodeConnectorRef
     * @param tableKey
     * @return flowTableIId
     */
    protected static InstanceIdentifier<Table> createTableIId(final NodeConnectorRef nodeConnectorRef, final TableKey tableKey) {

        return getNodeIId(nodeConnectorRef).builder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableKey))
                .build();
    }

    /**
     * Create odlmaple.flow iid by node connector ref, table key and odlmaple.flow key
     *
     * @param nodeConnectorRef
     * @param tableKey
     * @param flowId
     * @return flowIId
     */
    public static InstanceIdentifier<Flow> createFlowIId(final NodeConnectorRef nodeConnectorRef, final TableKey tableKey, final FlowId flowId) {

        return createTableIId(nodeConnectorRef, tableKey).child(Flow.class, new FlowKey(flowId));
    }

    /**
     * Create table instance identifier by flow Id
     *
     * @param flowIId
     * @return Table instance identifier
     */
    protected static InstanceIdentifier<Table> settableIId (InstanceIdentifier<Flow> flowIId) {

        return flowIId.<Table>firstIdentifierOf(Table.class);
    }
}
