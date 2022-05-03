package org.atsign.client.cli;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtSign;
import static org.atsign.common.KeyBuilders.*;

import org.atsign.common.Keys;
import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.common.AtException;

import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;

/**
 * A command-line interface half-example half-utility to share something with another atSign
 */
public class Share {
    public static void main(String[] args) {
        String rootUrl; // e.g. "vip.ve.atsign.zone:64";
        AtSign atSign;  // e.g. "@alice";
        AtSign otherAtSign;  // e.g. "@bob";
        String keyName;
        String toShare;
        int ttr;

        if (args.length != 6) {
            System.err.println("Usage: Share <rootUrl> <your AtSign> <other AtSign> <keyName to share, including namespace> <keyValue to share, a string> <ttr>");
            System.exit(1);
        }

        rootUrl = args[0];
        atSign = new AtSign(args[1]);
        otherAtSign = new AtSign(args[2]);
        keyName = args[3];
        toShare = args[4];
        ttr = Integer.parseInt(args[5]);

        // Let's also look up the other one before we do anything, just in case
        try {
            new AtRootConnection(rootUrl).lookupAtSign(otherAtSign);
        } catch (Exception e) {
            System.err.println("Failed to look up remote secondary for " + otherAtSign + " : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        AtClient atClient = null;
        try {
            atClient = AtClient.withRemoteSecondary(rootUrl, atSign);
        } catch (AtException e) {
            System.err.println("Failed to create AtClientImpl : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        try {
            SharedKeyBuilder sharedKeyBuilder = new SharedKeyBuilder(atSign, otherAtSign);
            sharedKeyBuilder = sharedKeyBuilder.cache(ttr, true);
            sharedKeyBuilder = sharedKeyBuilder.key(keyName);
            Keys.SharedKey sharedKey = sharedKeyBuilder.build();

            System.out.println(OffsetDateTime.now() + " | calling atClient.put()");
            String putResponse = atClient.put(sharedKey, toShare).get();
            System.out.println(OffsetDateTime.now() + " | put response : " + putResponse);
        } catch (AtException | InterruptedException | ExecutionException e) {
            System.err.println("Failed to share : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

}
