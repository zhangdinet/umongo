/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import org.xml.sax.SAXException;

/**
 *
 * @author antoine
 */
public class RouterNode extends BaseTreeNode {

    Mongo mongo;
    ServerAddress addr;

    public RouterNode(ServerAddress addr, Mongo mongo) throws IOException, SAXException {
        this.addr = addr;
        this.mongo = mongo;
        xmlLoad(Resource.getXmlDir(), Resource.File.routerNode, null);
    }

    @Override
    protected void populateChildren() {
        CommandResult res = mongo.getDB("admin").command("listShards");
        BasicDBList shards = (BasicDBList) res.get("shards");
        for (Object obj : shards) {
            try {
                DBObject shard = (DBObject) obj;
                String id = (String) shard.get("_id");
                String hosts = (String) shard.get("host");
                String repl = null;
                int slash = hosts.indexOf('/');
                if (slash >= 0) {
                    repl = hosts.substring(0, slash);
                    hosts = hosts.substring(slash + 1);
                }

                String[] hostList = hosts.split(",");
                ArrayList<ServerAddress> addrs = new ArrayList<ServerAddress>();
                for (String host : hostList) {
                    int colon = host.indexOf(':');
                    if (colon >= 0) {
                        addrs.add(new ServerAddress(host.substring(0, colon), Integer.parseInt(host.substring(colon + 1))));
                    } else {
                        addrs.add(new ServerAddress(host));
                    }
                }

                if (repl != null || addrs.size() > 1) {
                    addChild(new ReplSetNode(repl, addrs));
                } else {
                    addChild(new ServerNode(addrs.get(0)));
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, null, e);
            }
        }
    }

    public ServerAddress getAddress() {
        return addr;
    }

    public Mongo getMongo() {
        return mongo;
    }

    @Override
    protected void updateNode(List<ImageIcon> overlays) {
        label = getAddress().toString();
    }
}
