package com.pslcl.internal.app.mvnTree;

@SuppressWarnings("javadoc")
public class Node
{
    final String path;
    final boolean group;
    final boolean createPom;

    public Node(String path, boolean group, boolean createPom)
    {
        this.path = path.replace('\\', '/');
        this.group = group;
        this.createPom = createPom;
    }
}
