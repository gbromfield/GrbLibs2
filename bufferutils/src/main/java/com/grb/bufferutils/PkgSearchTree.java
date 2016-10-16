package com.grb.bufferutils;

import java.util.HashMap;

public class PkgSearchTree<T> {
    
    public class SearchTreeNode {
        public SearchTreeNode parent;
        public String name;
        public T data;
        public HashMap<String, SearchTreeNode> mChildren;
        
        public SearchTreeNode(SearchTreeNode parent, String name, T data) {
            this.parent = parent;
            this.name = name;
            this.data = data;
            mChildren = null;
        }

        public SearchTreeNode addChild(String name, T data) {
            if (mChildren == null) {
                mChildren = new HashMap<String, PkgSearchTree<T>.SearchTreeNode>();
            }
            SearchTreeNode newNode = new SearchTreeNode(this, name, data);
            mChildren.put(name, newNode);
            return newNode;
        }
        
        public SearchTreeNode getChild(String name) {
            if (mChildren == null) {
                return null;
            }
            return mChildren.get(name);
        }
        
        public boolean isRoot() {
            return (parent == null);
        }

        public String toString() {
            return name + "=" + data;
        }
        
        public void toString(StringBuilder bldr, String indent) {
            bldr.append(indent);
            bldr.append(toString());
            bldr.append("\r\n");
            if (mChildren != null) {
                String newIndent = indent + "    ";
                for(SearchTreeNode node : mChildren.values()) {
                    node.toString(bldr, newIndent);
                }
            }
        }
    }

    protected SearchTreeNode mRoot;
    
    public PkgSearchTree() {
        mRoot = new SearchTreeNode(null, "root", null);
    }

    public PkgSearchTree(T data) {
        mRoot = new SearchTreeNode(null, "root", data);
    }

    public void setRoot(T data) {
        mRoot.data = data;
    }
    
    public void addSearchItem(String str, T data) {
        SearchTreeNode currentNode = mRoot;
        String[] elems = str.split("\\.");
        for(int i = 0; i < elems.length; i++) {
            SearchTreeNode child = currentNode.getChild(elems[i]);
            if (child == null) {
                child = currentNode.addChild(elems[i], null);
            }
            currentNode = child;
        }
        currentNode.data = data;
    }
    
    public T getSearchItem(String str) {
        SearchTreeNode currentNode = mRoot;
        String[] elems = str.split("\\.");
        for(int i = 0; i < elems.length; i++) {
            SearchTreeNode child = currentNode.getChild(elems[i]);
            if (child == null) {
                break;
            }
            currentNode = child;
        }
        while(true) {
            if ((currentNode.isRoot()) || (currentNode.data != null)) {
                return currentNode.data;
            }
            currentNode = currentNode.parent;
        }
    }
    
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        mRoot.toString(bldr, "");
        return bldr.toString();
    }
    
    
    static public void main(String[] args) {
        PkgSearchTree<Integer> tree = new PkgSearchTree<Integer>();
        System.out.println(tree);
        tree.setRoot(1);
        tree.addSearchItem("java.lang.Integer", 2);
        System.out.println(tree);
        System.out.println(tree.getSearchItem("gaga"));
        System.out.println(tree.getSearchItem("java"));
        System.out.println(tree.getSearchItem("java.lang"));
        System.out.println(tree.getSearchItem("java.lang.Integer"));
        System.out.println(tree.getSearchItem("java.lang.Integer.gaga"));

        System.out.println(tree.getSearchItem("com"));
        System.out.println(tree.getSearchItem("com.solacesystems.Test"));

        tree.addSearchItem("com.solacesystems.Test", 3);
        System.out.println(tree);

        System.out.println(tree.getSearchItem("com"));
        System.out.println(tree.getSearchItem("com.solacesystems.Test"));
    }
}
