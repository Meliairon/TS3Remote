package meliarion.ts3.ts3remote;

import java.util.EnumSet;
import java.util.HashMap;

/**
 *An enum specifying the different kinds of notify message types
 * Created by James on 28/08/2015.
 */
public enum NotifyMessageType {
    invalid("invalid"),
    selected("selected"),
    notifytalkstatuschange("notifytalkstatuschange"),
    notifytextmessage("notifytextmessage"),
    notifyservergrouplist("notifyservergrouplist"),
    notifychannelgrouplist("notifychannelgrouplist"),
    notifyclientupdated("notifyclientupdated"),
    notifyclientmoved("notifyclientmoved"),
    notifyclientchannelgroupchanged("notifyclientchannelgroupchanged"),
    notifycurrentserverconnectionchanged("notifycurrentserverconnectionchanged"),
    notifyservergroupclientadded("notifyservergroupclientadded"),
    notifyclientleftview("notifyclientleftview"),
    notifycliententerview("notifycliententerview"),
    notifychannelcreated("notifychannelcreated"),
    notifychanneledited("notifychanneledited"),
    notifyserverupdated("notifyserverupdated"),
    notifyconnectstatuschange("notifyconnectstatuschange"),
    channellist("channellist"),
    notifyclientneededpermissions("notifyclientneededpermissions"),
    notifychanneldeleted("notifychanneldeleted"),
    notifychannelsubscribed("notifychannelsubscribed"),
    notifychannelpermlist("notifychannelpermlist"),
    notifyservergrouppermlist("notifyservergrouppermlist"),
    notifychannelmoved("notifychannelmoved"),
    notifychannelpasswordchanged("notifychannelpasswordchanged"),
    notifyserveredited("notifyserveredited"),
    notifyfilelist("notifyfilelist"),
    notifyfilelistfinished("notifyfilelistfinished"),
    notifychanneldescriptionchanged("notifychanneldescriptionchanged"),
    notifystartdownload("notifystartdownload"),
    channellistfinished("channellistfinished"),
    notifymutedclientdisconnected("notifymutedclientdisconnected"),
    notifychannelunsubscribed("notifychannelunsubscribed");

    private final String text;
    private static final HashMap<String, NotifyMessageType> Lookup = new HashMap<String, NotifyMessageType>();

    static {
        for (NotifyMessageType n : EnumSet.allOf(NotifyMessageType.class)) {
            Lookup.put(n.getText(), n);

        }
    }

    NotifyMessageType(final String type) {
        this.text = type;
    }

    @Override
    public String toString() {
        return this.getText();
    }

    public String getText() {
        return this.text;
    }

    public static NotifyMessageType getNotifyMessageType(String type) {
        if (Lookup.containsKey(type)) {
            return Lookup.get(type);
        }
        return invalid;
    }
}
