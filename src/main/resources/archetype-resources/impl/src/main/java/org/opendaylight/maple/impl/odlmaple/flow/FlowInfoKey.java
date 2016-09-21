/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.odlmaple.flow;

/**
 * FlowInfoKey is the key of a flowIId map, It contains a pair of match and nodeIId
 */
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FlowInfoKey {
    private Match match;
    private InstanceIdentifier<Node> nodeIId;
    protected FlowInfoKey(Match match, InstanceIdentifier<Node> nodeIId) {
        this.match = match;
        this.nodeIId = nodeIId;
    }
    protected FlowInfoKey(){}
}
