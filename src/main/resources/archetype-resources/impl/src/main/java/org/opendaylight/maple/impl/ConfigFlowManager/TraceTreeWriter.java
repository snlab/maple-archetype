/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.ConfigFlowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.maple.core.increment.tracetree.Drop;
import org.maple.core.increment.tracetree.Flood;
import org.maple.core.increment.tracetree.LNode;
import org.maple.core.increment.tracetree.Match;
import org.maple.core.increment.tracetree.Node;
import org.maple.core.increment.tracetree.Path;
import org.maple.core.increment.tracetree.Punt;
import org.maple.core.increment.tracetree.Rule;
import org.maple.core.increment.tracetree.TNode;
import org.maple.core.increment.tracetree.TraceTree;
import org.maple.core.increment.tracetree.VNode;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.tracetree.ttnode.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.tracetree.ttnode.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.tracetree.ttnode.link.DstNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.tracetree.ttnode.link.DstNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.tracetree.ttnode.link.SrcNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.tracetree.ttnode.link.SrcNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.tracetree.rev151114.Tracetree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.tracetree.rev151114.TracetreeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.tracetree.rev151114.tracetree.Ttlink;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.tracetree.rev151114.tracetree.TtlinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.tracetree.rev151114.tracetree.Ttnode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.tracetree.rev151114.tracetree.TtnodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class TraceTreeWriter {
	
	DataBroker dataProvider;
	List<Ttnode> ttNodeList;
	List<Ttlink> ttLinkList;
	int id;

	public TraceTreeWriter(final DataBroker dataProvider){
		this.dataProvider = dataProvider;
		this.ttNodeList = new ArrayList<Ttnode>();
		this.ttLinkList = new ArrayList<Ttlink>();
		this.id = 0;
	}
	
	private void cleanup() {
		ttNodeList.clear();
		ttLinkList.clear();
		this.id = 0;
	}
	
	private Ttnode generateTNode(String id, TNode tNode) {
		org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.t.type.rev151114.Ttnode1 tnode;
		tnode = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.t.type.rev151114.Ttnode1Builder()
				.setField(Match.toString(tNode.field)).build();
		Ttnode node = new TtnodeBuilder().addAugmentation(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.t.type.rev151114.Ttnode1.class,
				tnode).setId(id).setType("T").build();
		return node;
	}
	
	private Ttnode generateVNode(String id, VNode vNode) {
		org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.v.type.rev151114.Ttnode1 vnode;
		vnode = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.v.type.rev151114.Ttnode1Builder()
				.setField(Match.toString(vNode.field)).build();
		Ttnode node = new TtnodeBuilder().addAugmentation(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.v.type.rev151114.Ttnode1.class,
				vnode).setId(id).setType("V").build();
		return node;
	}
	
	private String getNodeFromTpId(String tpId) {
		// openflow:1:1 -> openflow:1
		String[] temps = tpId.split(":");
		String result = "";
		result = temps[0] + ":" + temps[1];
		return result;
	}
	
	private Ttnode generateLNode(String id, LNode lNode) {
		Rule rule = lNode.rule;
		org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.Ttnode1 lnode = null;
		if (rule.inst.getRouteAction() instanceof Path) {
			List<String> links = ((Path)rule.inst.getRouteAction()).links;
			List<Link> linksODL = new ArrayList<Link>();
			for (String linkString: links) {
				String linkId = Path.getLinkId(linkString);
				String srcTpId = Path.getSrcTpId(linkString);
				String dstTpId = Path.getDstTpId(linkString);
				String srcNode = getNodeFromTpId(srcTpId);
				String dstNode = getNodeFromTpId(dstTpId);
				SrcNode srcNodeODL = new SrcNodeBuilder().setNodeId(srcNode).setPort(srcTpId).build();
				DstNode dstNodeODL = new DstNodeBuilder().setNodeId(dstNode).setPort(dstTpId).build();
				Link link = new LinkBuilder().setLinkId(linkId).setSrcNode(srcNodeODL).setDstNode(dstNodeODL).build();
				linksODL.add(link);
			}
			lnode = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.Ttnode1Builder()
					.setActionType("Path").setLink(linksODL).build();
		} else if (rule.inst.getRouteAction() instanceof Drop) {
			lnode = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.Ttnode1Builder()
					.setActionType("Drop").setLink(new ArrayList<Link>()).build();
		} else if (rule.inst.getRouteAction() instanceof Flood) {
			lnode = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.Ttnode1Builder()
					.setActionType("Flood").setLink(new ArrayList<Link>()).build();
		} else if (rule.inst.getRouteAction() instanceof Punt) {
			lnode = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.Ttnode1Builder()
					.setActionType("Punt").setLink(new ArrayList<Link>()).build();
		}
		Ttnode node = new TtnodeBuilder().addAugmentation(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.l.type.rev151114.Ttnode1.class,
				lnode).setId(id).setType("L").build();
		return node;
	}
	
	// condition can be "== 10.0.0.1", "10.0.0.1", "!= 10.0.0.1"
	private Ttlink generateTtLink(String linkId, String pId, String dId, String condition) {
		Ttlink ttLink = new TtlinkBuilder().setId(linkId).setPredicateId(pId)
				.setDestinationId(dId).setCondition(condition).build();
		return ttLink;
	}
	
	private String traceTreeTraversal(Node node) {
		String newId = String.valueOf(id++);
		
		if (node instanceof TNode) {
			TNode tNode = (TNode)node;
			Ttnode ttNode = generateTNode(newId, tNode);
			this.ttNodeList.add(ttNode);
			if (tNode.getChild(true) != null) {
				//true branch
				String childId = traceTreeTraversal(tNode.getChild(true));
				String linkId = String.valueOf(id++);
				Ttlink ttLink = generateTtLink(linkId, newId, childId, "== " + tNode.value);
				this.ttLinkList.add(ttLink);
			}
			if (tNode.getChild(false) != null) {
				//false branch
				String childId = traceTreeTraversal(tNode.getChild(false));
				String linkId = String.valueOf(id++);
				Ttlink ttLink = generateTtLink(linkId, newId, childId, "!= " + tNode.value);
				this.ttLinkList.add(ttLink);
			}
		} else if (node instanceof VNode) {
			VNode vNode = (VNode)node;
			Ttnode ttNode = generateVNode(newId, vNode);
			this.ttNodeList.add(ttNode);
			for(Map.Entry<String, Node> entry: vNode.subtree.entrySet()) {
				String fieldValue = entry.getKey();
				Node childNode = entry.getValue();
				String childId = traceTreeTraversal(childNode);
				String linkId = String.valueOf(id++);
				Ttlink ttLink = generateTtLink(linkId, newId, childId, fieldValue);
				this.ttLinkList.add(ttLink);
			}
		} else {
			// node should be LNode
			LNode lNode = (LNode)node;
			Ttnode ttNode = generateLNode(newId, lNode);
			this.ttNodeList.add(ttNode);
		}
		return newId;
	}
	
	private synchronized void submit(final WriteTransaction writeTx) {
        final CheckedFuture writeTxResultFuture = writeTx.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
            	//System.out.println("write tt success");
            }

            @Override
            public void onFailure(Throwable throwable) {
            	//System.out.println("write tt fail");
            }
        });
    }


	public synchronized void writeToDataStore(TraceTree tt){
		if(tt == null)return;
		cleanup();

		InstanceIdentifier<Tracetree> TT = InstanceIdentifier.builder(Tracetree.class).build();
	    Node root = tt.root;
	    traceTreeTraversal(root);

	    /*MapleApps mas = new MapleAppsBuilder().setTtlinks(ttLinksList).setTtnodes(ttNodesList).setAppName("app name").build();
	    List<MapleApps> mapleAppsList = new ArrayList<MapleApps>();
	    mapleAppsList.add(mas);*/
	    Tracetree ttODL = new TracetreeBuilder().setTtlink(this.ttLinkList).setTtnode(ttNodeList).build();

	    final WriteTransaction wt = this.dataProvider.newWriteOnlyTransaction();
	    try {
            wt.put(LogicalDatastoreType.CONFIGURATION, TT, ttODL, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

	    submit(wt);
	}
}
