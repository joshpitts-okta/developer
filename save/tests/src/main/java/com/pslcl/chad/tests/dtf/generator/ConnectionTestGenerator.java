package com.pslcl.chad.tests.dtf.generator;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.ec2.model.InstanceType;
import com.pslcl.chad.tests.dtf.generator.util.Util;
import com.pslcl.chad.tests.dtf.generator.util.Util.Library;
import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.resource.Cable;
import com.pslcl.dtf.core.generator.resource.Machine;
import com.pslcl.dtf.core.generator.resource.Network;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;

public class ConnectionTestGenerator
{
	public static void main(String[] args) throws Exception
	{
		String DTF_TEST_ID = System.getenv("DTF_TEST_ID");
		int qa_test_id = Integer.parseInt(DTF_TEST_ID);
		Generator G = new Generator(qa_test_id);

		Attributes attributes = new Attributes();
        attributes.put(ProviderNames.InstanceTypeKey, InstanceType.M3Medium.toString());
        attributes.put(ProviderNames.LocationFeatureKey, ProviderNames.LocationJava7);

		Library[] libs = new Library[] { Library.Java, Library.C, Library.CSharp };
		Iterable<Artifact[]> serverVersions = Util.getVersions(G, libs, "GenericServer");
		Iterable<Artifact[]> asVersions = Util.getVersions(G, libs, "GenericAS");
		Iterable<Artifact[]> requestorVersions = Util.getVersions(G, libs, "ConnectionTest");

		List<String> flags = Arrays.asList(args);
		boolean useAS = flags.contains("-as");
		boolean useUDP = flags.contains("-udp");
		boolean useMcast = flags.contains("-mcast");
		boolean useStateless = flags.contains("-stateless");
		boolean useSecure = flags.contains("-secure");
		boolean useAsync = flags.contains("-async");
		boolean useShare = flags.contains("-share");

		for (Artifact[] server : serverVersions)
		{
			for (Artifact[] requestor : requestorVersions)
			{
				if (!useAS)
				{
					G.startTest();
					Network network = new Network(G, "ConnectionTest");
					network.bind();

					generate(G, network, attributes, server, requestor,
						null, useUDP, useMcast, useStateless, useSecure, useAsync, useShare);

					G.completeTest();
				}
				else
				{
					for (Artifact[] as : asVersions)
					{
						G.startTest();
						Network network = new Network(G, "ConnectionTest");
						network.bind();

						Machine asMachine = Util.buildMachine(G, "as", attributes, as);
						Cable asCable = asMachine.connect(network);
						asMachine.run(as[0], "-doflog", "none");

						generate(G, network, attributes, server, requestor,
							asCable, useUDP, useMcast, useStateless, useSecure, useAsync, useShare);

						G.completeTest();
					}
				}
			}
		}

		G.close();
	}

	private static void generate(Generator G, Network network, Attributes attributes, Artifact[] server, Artifact[] requestor,
		Cable asCable, boolean useUDP, boolean useMcast, boolean useStateless, boolean useSecure, boolean useAsync, boolean useShare)
		throws Exception
	{
		Machine serverMachine = Util.buildMachine(G, "server", attributes, server);
		Cable serverCable = serverMachine.connect(network);
		serverMachine.run(server[0], "-doflog", "none",
			(asCable != null) ? "-secure -asaddress " + asCable.getIPReference() + "-asconnection" : "");

		Machine requestorMachine = Util.buildMachine(G, "requestor", attributes, requestor);
		requestorMachine.connect(network);

		String ip;
		if (useMcast)
			ip = "224.0.23.46 -port 5567";
		else if (asCable != null)
			ip = asCable.getIPReference() + " -asconnection";
		else
			ip = serverCable.getIPReference();

		requestorMachine.run(requestor[0],
			"-connectionNum", "100", "-logPeriod", "60", "-doflog", "none", "-maxminutes", "2",
			"-address", ip,
			(asCable != null || useSecure) ? "-secure" : "",
			(useUDP || useMcast || useStateless) ? "-udp" : "",
			useStateless ? "-stateless" : "",
			useAsync ? "-asyncconnection" : "",
			useShare ? "-share" : "");
	}
}
