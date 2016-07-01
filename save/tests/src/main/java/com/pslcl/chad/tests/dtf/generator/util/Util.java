package com.pslcl.chad.tests.dtf.generator.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.resource.Machine;

public class Util
{
	public enum Library
	{
		Java, C, CSharp, COS
	}

	public static Iterable<Artifact[]> getVersions(Generator G, Library[] libs, String... scripts)
	{
		List<Library> langs = Arrays.asList(libs);

		ArrayList<Iterable<Artifact[]>> artifacts = new ArrayList<Iterable<Artifact[]>>(4);

		List<String> binPaths = Arrays.asList(scripts);
		for (int i = 0; i < binPaths.size(); i++)
			binPaths.set(i, "bin/" + binPaths.get(i));

		if (langs.contains(Library.Java))
		{
			ArrayList<String> paths = new ArrayList<String>(binPaths);
			paths.add("lib/testing-dof-oal-.+\\.jar");
			artifacts.add(G.createArtifactSet(null, null, paths.toArray(new String[paths.size()])));
		}

		if (langs.contains(Library.C))
		{
			ArrayList<String> paths = new ArrayList<String>(binPaths);
			for (String script : scripts)
				paths.add("libexec/" + script);
			artifacts.add(G.createArtifactSet(null, null, paths.toArray(new String[paths.size()])));
		}

		if (langs.contains(Library.CSharp))
		{
			ArrayList<String> paths = new ArrayList<String>(binPaths);
			for (int i = 0; i < paths.size(); i++)
				paths.set(i, paths.get(i) + "\\.exe");
			artifacts.add(G.createArtifactSet(null, null, paths.toArray(new String[paths.size()])));
		}

		if (langs.contains(Library.COS))
		{
			ArrayList<String> paths = new ArrayList<String>(binPaths);
			for (String script : scripts)
				paths.add("libexec/cos/" + script);
			artifacts.add(G.createArtifactSet(null, null, paths.toArray(new String[paths.size()])));
		}

		return Iterables.concat(artifacts);
	}

	public static Machine buildMachine(Generator G, String name, Attributes attributes, Artifact[] artifacts)
		throws Exception
	{
		Machine machine = new Machine(G, name);
		machine.bind(new Attributes(attributes).putAll(artifacts[0].getModule().getAttributes()));
		machine.deploy(artifacts);
		return machine;
	}

	public static String[] buildArgs(String[] cmd, String... base)
	{
		ArrayList<String> args = new ArrayList<String>(Arrays.asList(base));
		Collections.addAll(args, cmd);
		return args.toArray(new String[args.size()]);
	}
}
