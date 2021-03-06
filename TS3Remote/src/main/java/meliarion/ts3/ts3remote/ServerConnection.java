package meliarion.ts3.ts3remote;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Created by Meliarion on 28/07/13.
 * Class that represents a connection to a server
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class ServerConnection {
    private String Name="Unknown server";
    private String OldServerChat = "";
    private List<ChatMessage> ServerChat = new ArrayList<ChatMessage>();
    private String OldChannelChat = "";
    private List<ChatMessage> ChannelChat = new ArrayList<ChatMessage>();
    String OldPrivateChat = "";
    private List<ChatMessage> PrivateChat = new ArrayList<ChatMessage>();
    private boolean ChannelsReceived = false;
    private boolean ConnectionTested = false;
    private boolean ConnectionVerified =false;
    private boolean ClientsReceived = false;
    private boolean SetupDone =false;
    private int clientID=-1;
    private int id;
    private int port;
    private int iconID;
    private String address;
    private final List<TSClient> ClientList = new ArrayList<TSClient>();
    private final List<TSGroup> ServerGroupList = new ArrayList<TSGroup>();
    private final List<TSGroup> ChannelGroupList = new ArrayList<TSGroup>();
    private final List<TSChannel> ChannelList = new ArrayList<TSChannel>();
    //private ConnectionStage stage;
    private final List<Integer> unprocessedChannels = new ArrayList<Integer>();
    private final Map<Integer, Integer> ClientMap = new TreeMap<Integer, Integer>();
    private final Map<Integer, Integer> ServerGroupMap = new TreeMap<Integer, Integer>();
    private final Map<Integer, Integer> ChannelGroupMap = new TreeMap<Integer, Integer>();
    private final Map<Integer, Integer> ChannelMap = new TreeMap<Integer, Integer>();
    private final Map<Integer, List<Integer>> ChannelToClientMap = new TreeMap<Integer, List<Integer>>();
    private final List<ItemNotFoundType> Requests = new ArrayList<ItemNotFoundType>();


    ServerConnection(int _id){
        this.id = _id;
        initialiseChannels();
    }

    private void initialiseChannels(){
        this.ChannelList.add(new TSChannel(0,-1,"Server top level",TSChannel.ChannelType.ServerTopLevel));
        this.ChannelMap.put(0, 0);
    }
    public static String makeTransmitSafe (String original)
    {
        Pattern space = Pattern.compile("\\s");
        return space.matcher(original).replaceAll("\\s");
    }
    public static String unmakeTransmitSafe (String original)
    {
        Pattern space = Pattern.compile("\\Q\\s\\E");
        return space.matcher(original).replaceAll(" ");
    }
    @Override
    public String toString() {
        if (Name != null){
        return Name +": SCHandlerID"+id;
        }
        else
        {
        return "SCHandlerID="+id;
        }
    }
    public int getID(){
        return  id;
    }

    public int getClientID() {
        return clientID;
    }
    public ConnectionStage getStage(){
        //boolean b = hasChannels()&&(unprocessedChannels.size()==0)&&(ChannelList.get(0).getSubchannelIDs().size()>0);
        if(ConnectionTested&&!ChannelsReceived&&!ConnectionVerified&&!ClientsReceived &&!hasServerGroups()&&!hasChannelGroups())
        {
            return ConnectionStage.Disconnected;
        }
        else if (!(ConnectionVerified))
        {
        return ConnectionStage.VerifyConnection;
        }
        else  if (ConnectionVerified&&!hasServerGroups())
        {
        return ConnectionStage.RequestServerGroups;
        }
        else if (ConnectionVerified&&hasServerGroups()&&!hasChannelGroups())
        {
            return ConnectionStage.RequestChannelGroups;
        }
        else if (ConnectionVerified&&hasServerGroups()&&hasChannelGroups()&&!ClientsReceived)
        {
            return ConnectionStage.RequestClients;
        }
        else if(ConnectionVerified&&hasServerGroups()&&hasChannelGroups()&& ClientsReceived &&!ChannelsReceived)
        {
            return ConnectionStage.RequestChannels;
        }
        else if(ConnectionVerified&&hasServerGroups()&&hasChannelGroups()&& ClientsReceived &&ChannelsReceived&&!SetupDone)
        {
            return ConnectionStage.SetupDone;
        }
        else if(ConnectionVerified&&hasServerGroups()&&hasChannelGroups()&& ClientsReceived &&ChannelsReceived&&SetupDone)
        {
            return ConnectionStage.SetupDone;
        }
        else
        {
            return ConnectionStage.InvalidStage;
        }

    }

    private void AddServerMessage(String message, String sender, String senderUID, int senderID) {
        OldServerChat += sender + ":" + message + System.getProperty("line.separator");
        Calendar now = Calendar.getInstance();
        ChatMessage newMessage = new ChatMessage(unmakeTransmitSafe(message), senderID, now.getTimeInMillis());
        ServerChat.add(newMessage);
    }

    private void AddChannelMessage(String message, String sender, String senderUID, int senderID) {
        OldChannelChat += sender + ":" + message + System.getProperty("line.separator");
        Calendar now = Calendar.getInstance();
        ChatMessage newMessage = new ChatMessage(unmakeTransmitSafe(message), senderID, now.getTimeInMillis());
        ChannelChat.add(newMessage);
    }
    public boolean isConnected(){
        ConnectionStage stage = getStage();
        return stage==ConnectionStage.SetupDone;
    }
    public boolean Connecting(){
        return getStage()!=ConnectionStage.Disconnected;
    }

    public void Disconnect(){
        ChannelsReceived = false;
        ConnectionVerified =false;
        ClientsReceived = false;
        ConnectionTested = true;
        SetupDone=false;
        ClearServerGroups();
        ClearChannelGroups();
        ClearClientList();
        ClearChannelList();
        initialiseChannels();
    }

    public void Reinitialise(){
    if(this.getStage()!=ConnectionStage.Disconnected)
    {
        Disconnect();
    }
    ConnectionTested = false;
    }
    public boolean hasServerGroups(){
        return (ServerGroupList.size()>0);
    }

    public boolean hasChannelGroups(){
        return (ChannelGroupList.size()>0);
    }
    public boolean hasClients(){
        return (ClientList.size()>0);
    }
    public boolean hasChannels(){
        return (ChannelList.size()>1);
    }
    public  void connectionVerified()
    {
        ConnectionVerified = true;
        ConnectionTested = true;
    }

    public void AddTextMessage(String message, int type, String sender, String senderUID, int senderID) {
        switch (type){
            case 0:
            default:
            case 1:
                Log.e("ServerConnection", "Invalid chat message");
            break;
            case 2:
                AddChannelMessage(message, sender, senderUID, senderID);
            break;
            case 3:
                AddServerMessage(message, sender, senderUID, senderID);
            break;
        }
    }

    public void AddTextMessage(String message, int type, String sender, String senderUID, int senderID, int target) throws SCNotFoundException {
        switch (type) {
            case 1:
                AddPrivateMessage(message, sender, senderUID, senderID, target);
                break;
            default:
                AddTextMessage(message, type, sender, senderUID, senderID);
                break;
        }

    }

    private void AddPrivateMessage(String message, String sender, String senderUID, int senderID, int target) throws SCNotFoundException {
        OldPrivateChat += sender + ":" + message + System.getProperty("line.separator");
        Calendar now = Calendar.getInstance();
        ChatMessage newMessage = new ChatMessage(message, senderID, now.getTimeInMillis());
        PrivateChat.add(newMessage);
        if (senderID == clientID) {
            TSClient TargetClient = getClientByCLID(target);
            TargetClient.addPrivateChatMessage(newMessage);
        } else if (target == clientID) {
            TSClient SenderClient = getClientByCLID(senderID);
            SenderClient.addPrivateChatMessage(newMessage);
        } else {
            Log.e("ServerConnection", "Invalid private message recieved.");
        }
    }

    public String getServerChat() {
        try {
            String result = "";


            for (ChatMessage chat : ServerChat) {
                result += makeChatString(chat);
            }
            return result;//unmakeTransmitSafe(OldServerChat);
        } catch (SCNotFoundException ex) {
            String s = "Error retrieving server chat";
            Log.e("ServerConnection", s, ex);
            return s;
    }
    }

    private String makeChatString(ChatMessage message) throws SCNotFoundException {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        c.setTimeInMillis(message.getTimestamp());
        String tmp = "<" + sdf.format(c.getTime()) + ">"; //c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND)+"> ";
        TSClient client = getClientByCLID(message.getSender());
        tmp += '"' + client.getName() + '"';
        tmp += ": " + message.getMessage() + System.getProperty("line.separator");
        return tmp;
    }

    public String getChannelChat() {   //return unmakeTransmitSafe(OldChannelChat);
        try {
            String result = "";


            for (ChatMessage chat : ChannelChat) {
                result += makeChatString(chat);
            }
            return result;
        } catch (SCNotFoundException ex) {
            String s = "Error retrieving server chat";
            Log.e("ChannelConnection", s, ex);
            return s;
        }
    }

    public String getPrivateChat(int clid) {

        try {
            String result = "";
            TSClient client = getClientByCLID(clid);


            for (ChatMessage chat : client.getPrivateChat()) {
                result += makeChatString(chat);
            }
            return result;
        } catch (SCNotFoundException ex) {
            String s = "Error retrieving private chat for client " + clid;
            Log.e("ChannelConnection", s, ex);
            return s;
        }
    }
    public int getChannelCount(){
        return ChannelList.size();
    }
    public int getClientCount(){
        return ClientList.size();
    }

    public void AddClient(TSClient client){
        if (!ClientMap.containsKey(client.getClientID())) {
            ClientList.add(client);
            ClientMap.put(client.getClientID(), ClientList.size() - 1);
            List<Integer> cList;
            if ((cList = ChannelToClientMap.get(client.getChannelID())) == null) {
                ChannelToClientMap.put(client.getChannelID(), cList = new ArrayList<Integer>());
            }
            cList.add(client.getClientID());
            Collections.sort(cList, new ClientOrderer(this));
        } else {
            Log.e("ServerConnection", "Unable to add client, a client with that ID is already present");
        }
    }
    public void AddClient(Map<String,String> params)
    {TSClient client = new TSClient(params);
     AddClient(client);
    }
    public void AddClient(Map<String,String> params, int index)
    {
        TSClient client = new TSClient(params,index);
        AddClient(client);
    }

    public void AddServerGroup(TSGroup group){
        TSGroup grp = null;
        try{
        grp = getServerGroupByID(group.getID());
        }
        catch (SCNotFoundException ex)
        {
        Log.i("ServerConnection", "Server group id:"+group.getID()+" not found, creating group",ex);
        }
        finally {
            if (grp==null)
            {
                ServerGroupList.add(group);
                ServerGroupMap.put(group.getID(),ServerGroupList.size()-1);
            }
            else if(!grp.equals(group))
            {
                grp.updateGroup(group);
            }
        }
    }
    public void AddServerGroupByParams(Map<String,String> grpMap) {
    TSGroup grp = null;
        try{
        grp = getServerGroupByID(Integer.valueOf(grpMap.get("sgid")));
        }
        catch (SCNotFoundException ex)
        {
            Log.i("ServerConnection", "Server group id:"+grpMap.get("sgid")+" not found, creating group",ex);
        }
        finally {
        if (grp ==null){
            ServerGroupList.add(new TSGroup(grpMap));
            ServerGroupMap.put(Integer.valueOf(grpMap.get("sgid")),ServerGroupList.size()-1);
        }
        else
        {
         grp.updateGroup(grpMap);
        }
        }
    }

    public void AddChannelGroup (TSGroup group)
    {   ChannelGroupList.add(group);
        ChannelGroupMap.put(group.getID(),ChannelGroupList.size()-1);
    }

    public void AddChannel (TSChannel channel){
        int pid = channel.getParentID();
        int id = channel.getID();
        TSChannel chan = null;
        try{
        chan = getChannelByID(id);
        }
        catch (SCException ex){
            Log.i("ServerConnection", "Channel group id:"+id+" not found, creating channel",ex);
        }
        finally {
            if (chan==null)
            {
                 ChannelList.add(channel);
                 ChannelMap.put(channel.getID(),ChannelList.size()-1);
            }
            else if(!(channel==chan))
            {
                chan.updateChannel(channel);
            }
        }

        try{
        getChannelByID(pid).addSubchannel(id);
        }
        catch (SCException ex)
        {
            unprocessedChannels.add(id);
        }

    }
    public void processChannels(){
        for (int i :unprocessedChannels)
        {
            try{
                TSChannel chan = getChannelByID(i);
                TSChannel parent = getChannelByID(chan.getParentID());
                parent.addSubchannel(i);
            }
            catch (Exception e)
            {
                Log.e("ServerConnection", "Failed to process channel: ", e);
            }
        }
        unprocessedChannels.clear();
        ChannelOrderer orderer = new ChannelOrderer(this);
        for (TSChannel c : ChannelList){
            c.sortSubchannels(orderer);
        }
    }
    public void ClearServerGroups()
    {if (ServerGroupList.size()>0){
        ServerGroupList.clear();
        ServerGroupMap.clear();
    }
    }
    public void ClearChannelGroups(){
        if(ChannelGroupList.size()>0)
        {
            ChannelGroupList.clear();
            ChannelGroupMap.size();
        }
    }
    public void ClearClientList(){
        if(ClientList.size()>0)
        {
            ClientList.clear();
            ClientMap.clear();
        }
    }
    public void ClearChannelList(){
        if(ChannelList.size()>0){
            ChannelList.clear();
            ChannelMap.clear();
            initialiseChannels();
        }
    }
    public void removeClientByCLID(int clid) throws SCNotFoundException {
            TSClient client = getClientByCLID(clid);
            ChannelToClientMap.get(client.getChannelID()).remove(Integer.valueOf(clid));
            //ClientList.remove(client);
        ClientMap.remove(clid);

    }
    public void removeChannelByID(int cid) throws SCNotFoundException {
        TSChannel channel = getChannelByID(cid);
        ChannelToClientMap.remove(cid);
        ChannelList.remove(channel);
        ChannelMap.remove(cid);

    }
    public void removeServerGroupByID(int sgid) throws SCNotFoundException {
        TSGroup sGroup = getServerGroupByID(sgid);
        ServerGroupList.remove(sGroup);
        ServerGroupMap.remove(sgid);
    }
    public void removeChannelGroupByID(int cgid) throws SCNotFoundException {
        TSGroup cGroup = getChannelGroupByID(cgid);
        ChannelGroupList.remove(cGroup);
        ChannelGroupMap.remove(cgid);

    }
    public TSClient getClientByCLID(int clientID) throws SCNotFoundException
    {
        int index;
        if ((ClientMap.containsKey(clientID))&&((index=ClientMap.get(clientID))!=-1)){
        return ClientList.get(index);
        }
        throw new SCNotFoundException("client id="+clientID+" not found", ItemNotFoundType.Client);

    }
    public TSGroup getServerGroupByID(int ID) throws SCNotFoundException {
    try{
     int index;
        if ((ServerGroupMap.containsKey(ID))&&((index=ServerGroupMap.get(ID))!=-1)){
            return ServerGroupList.get(index);
        }
        throw new SCNotFoundException("ServerGroup id="+ID+" not found", ItemNotFoundType.ServerGroup);
    }
    catch (Exception ex)
    {
        throw new SCNotFoundException("ServerGroup id="+ID+" not found", ItemNotFoundType.ServerGroup,ex);
    }
    }
    public TSGroup getChannelGroupByID(int ID) throws SCNotFoundException {
        try {
            int index = ChannelGroupMap.get(ID);
            if (index != -1)
            {
                return ChannelGroupList.get(index);
            }
            throw new SCNotFoundException("ChannelGroup id="+ID+" not found", ItemNotFoundType.ChannelGroup);
        }
        catch (Exception ex)
        {
            throw new SCNotFoundException("ChannelGroup id="+ID+" not found", ItemNotFoundType.ChannelGroup, ex);
        }

    }
    public TSChannel getChannelByID(int ID) throws SCNotFoundException {
         try{
             int index = ChannelMap.get(ID);
            if (index != -1){
             return ChannelList.get(index);
            }
             throw new SCNotFoundException("Channel ID="+ID+" not found", ItemNotFoundType.Channel);
            }
            catch (Exception ex)
            {
            throw new SCNotFoundException("Channel ID="+ID+" not found", ItemNotFoundType.Channel,ex);
            }
    }
    public List<Integer> getClientsByChannelID(int cid){
        return ChannelToClientMap.get(cid);
    }
    public void moveClient(int clid, Map<String,String> params) throws SCNotFoundException {
        TSClient client = getClientByCLID(clid);
        ChannelToClientMap.get(client.getChannelID()).remove(new Integer(clid));
        client.moveClient(params);
        List<Integer> cList= ChannelToClientMap.get(client.getChannelID());
        if(cList==null){
            ChannelToClientMap.put(client.getChannelID(), cList = new ArrayList<Integer>());
        }
        cList.add(client.getClientID());
    }
    private static String parseParams (Map<String,String> params,String key)
    {String wanted;
        if (params.containsKey(key))
        {
            wanted =params.get(key);
            params.remove(key);
            return wanted;
        }
    return null;
    }

    @SuppressWarnings({"redundant", "UnusedAssignment"})
    public void updateServer(Map<String,String> params){
        String value;
        if((value=(parseParams(params,"virtualserver_port")))!=null)
        {
          this.port=Integer.valueOf(value);
        }
        if((value=(parseParams(params,"virtualserver_clientsonline")))!=null)
        {
            int clients=Integer.valueOf(value);

        }
        if((value=(parseParams(params,"virtualserver_channelsonline")))!=null)
        {
            int channels=Integer.valueOf(value);

        }
        if((value=(parseParams(params,"virtualserver_welcomemessage")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_welcomemessage = value;

        }
        if((value=(parseParams(params,"virtualserver_maxclients")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_maxclients = value;

        }
        if((value=(parseParams(params,"virtualserver_uptime")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_uptime = value;

        }
        if((value=(parseParams(params,"virtualserver_hostmessage")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_hostmessage = value;

        }
        if((value=(parseParams(params,"virtualserver_hostmessage_mode")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_hostmessage_mode = value;

        }
        if((value=(parseParams(params,"virtualserver_flag_password")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_flag_password = value;

        }
        if((value=(parseParams(params,"virtualserver_default_channel_admin_group")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_default_channel_admin_group = value;

        }
        if((value=(parseParams(params,"virtualserver_max_download_total_bandwidth")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_max_download_total_bandwidth = value;

        }
        if((value=(parseParams(params,"virtualserver_complain_autoban_count")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_complain_autoban_count = value;

        }
        if((value=(parseParams(params,"virtualserver_complain_autoban_time")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_complain_autoban_time = value;

        }
        if((value=(parseParams(params,"virtualserver_complain_remove_time")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_complain_remove_time = value;

        }
        if((value=(parseParams(params,"virtualserver_min_clients_in_channel_before_forced_silence")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_min_clients_in_channel_before_forced_silence = value;

        }
        if((value=(parseParams(params,"virtualserver_antiflood_points_tick_reduce")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_antiflood_points_tick_reduce = value;

        }
        if((value=(parseParams(params,"virtualserver_antiflood_points_needed_command_block")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_antiflood_points_needed_command_block = value;

        }
        if((value=(parseParams(params,"virtualserver_antiflood_points_needed_ip_block")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_antiflood_points_needed_ip_block = value;

        }
        if((value=(parseParams(params,"virtualserver_client_connections")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_client_connections = value;

        }
        if((value=(parseParams(params,"virtualserver_query_client_connections")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_query_client_connections = value;

        }
        if((value=(parseParams(params,"virtualserver_queryclientsonline")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_queryclientsonline = value;

        }
        if((value=(parseParams(params,"virtualserver_download_quota")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_download_quota = value;

        }
        if((value=(parseParams(params,"virtualserver_upload_quota")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_upload_quota = value;

        }
        if((value=(parseParams(params,"virtualserver_month_bytes_downloaded")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_month_bytes_downloaded = value;

        }
        if((value=(parseParams(params,"virtualserver_month_bytes_uploaded")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_month_bytes_uploaded = value;

        }
        if((value=(parseParams(params,"virtualserver_max_upload_total_bandwidth")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_max_upload_total_bandwidth = value;

        }
        if((value=(parseParams(params,"virtualserver_total_bytes_downloaded")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_total_bytes_downloaded = value;

        }
        if((value=(parseParams(params,"virtualserver_total_bytes_uploaded")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_total_bytes_uploaded = value;

        }
        if((value=(parseParams(params,"virtualserver_autostart")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_autostart = value;

        }
        if((value=(parseParams(params,"virtualserver_machine_id")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_machine_id = value;

        }
        if((value=(parseParams(params,"virtualserver_needed_identity_security_level")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_needed_identity_security_level = value;

        }
        if((value=(parseParams(params,"virtualserver_log_client")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_log_client = value;

        }
        if((value=(parseParams(params,"virtualserver_log_query")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_log_query = value;

        }
        if((value=(parseParams(params,"virtualserver_log_channel")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_log_channel = value;

        }
        if((value=(parseParams(params,"virtualserver_log_permissions")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_log_permissions = value;

        }
        if((value=(parseParams(params,"virtualserver_log_server")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_log_server = value;

        }
        if((value=(parseParams(params,"virtualserver_log_filetransfer")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_log_filetransfer = value;

        }
        if((value=(parseParams(params,"virtualserver_min_client_version")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_min_client_version = value;

        }
        if((value=(parseParams(params,"virtualserver_reserved_slots")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_reserved_slots = value;

        }
        if((value=(parseParams(params,"virtualserver_antiflood_points_needed_ip_block")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_antiflood_points_needed_ip_block = value;

        }
        if((value=(parseParams(params,"virtualserver_total_packetloss_speech")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_total_packetloss_speech = value;

        }
        if((value=(parseParams(params,"virtualserver_total_packetloss_keepalive")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_total_packetloss_keepalive = value;

        }
        if((value=(parseParams(params,"virtualserver_total_packetloss_control")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_total_packetloss_control = value;

        }
        if((value=(parseParams(params,"virtualserver_total_packetloss_total")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_total_packetloss_total = value;

        }
        if((value=(parseParams(params,"virtualserver_total_ping")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_total_ping = value;

        }
        if((value=(parseParams(params,"virtualserver_weblist_enabled")))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String virtualserver_weblist_enabled = value;

        }
        if (params.size()>0)
        {String error = params.size()+" unhandled parameters received  in server update. ";
            for (String s : params.keySet())
            {
                error += s+", ";
            }
            Log.w("ServerConnection",error);
        }
    }
    private String DisplayClient(TSClient client){
        try{
        String sgroups = "(";
        for(int i :client.getServerGroups()){
            sgroups += unmakeTransmitSafe(getServerGroupByID(i).getName())+",";
        }
        sgroups +=")";
        String cgroup = getChannelGroupByID(client.getChannelGroupID()).getName();
        return client.getStatus().toString()+" "+unmakeTransmitSafe(client.getName())+" "+sgroups+" "+unmakeTransmitSafe(cgroup)+" "+System.getProperty("line.separator");
        }
        catch (Exception e)
        {
            return "Error obtaining groups:"+e;
        }
    }
    private String DisplayChannel (TSChannel channel, String indent)throws SCNotFoundException{
        indent += "      ";
        String s;
        s = indent + ServerConnection.unmakeTransmitSafe(channel.getName())+System.getProperty("line.separator");
        List<Integer> clientIDs = getClientsByChannelID(channel.getID());
        if(clientIDs != null){
            for(int i :clientIDs)
            {
                TSClient client = getClientByCLID(i);
                s +=DisplayClient(client);
            }
        }
        TreeSet<TSChannel> subchannels = new TreeSet<TSChannel>();
        for(int i : channel.getSubchannelIDs())
        {
            subchannels.add(getChannelByID(i));
        }
        for (TSChannel c : subchannels)
        {
            s+=DisplayChannel(c,indent);
        }
        return s;
    }
    public String DisplayServer(){
        String s = "";
        String indent = "      ";

        try {
        s += "Server toplevel"+System.getProperty("line.separator");
            s+=DisplayChannel(getChannelByID(0), indent);
        }
        catch (Exception e){
            s = "an error occoured displaying the server:"+e;
        }
        return s;
    }
    public void setServerIconID(int _ID)
    {   try{
        this.iconID = _ID;
        TSChannel toplevel = this.getChannelByID(0);
        toplevel.setIcon(_ID);
        }
        catch (Exception ex)
        {
            Log.e("ServerConnection", "Error setting server icon",ex);
        }
    }

    public void moveChannel(int cid, int cpid) throws SCNotFoundException{
        TSChannel channel = getChannelByID(cid);
        TSChannel oldParent = getChannelByID(channel.getParentID());
        TSChannel newParent = getChannelByID(cpid);
        oldParent.getSubchannelIDs().remove(Integer.valueOf(cid));
        newParent.addSubchannel(cid);
        channel.changeParentID(cpid);
        Collections.sort(newParent.getSubchannelIDs(),new ChannelOrderer(this));
    }

    public void edited(Map<String, String> params) {
        String value;
        if((value=parseParams(params,"virtualserver_icon_id"))!=null)
        {
            setServerIconID(Integer.valueOf(value));
        }
        if((value=parseParams(params,"reasonid"))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String reasonid = value;
        }
        if((value=parseParams(params,"invokerid"))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String invokerid = value;
        }if((value=parseParams(params,"invokername"))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String invokername = value;
        }if((value=parseParams(params,"invokeruid"))!=null)
        {
            @SuppressWarnings("UnnecessaryLocalVariable") String invokeruid = value;
        }
        if (params.size()>0)
        {String error = params.size()+" unhandled parameters received  in server edited. ";
                for (String s : params.keySet())
                {
                    error += s+", ";
                }
            Log.w("ServerConnection",error);

        }
    }

    public void sortChannelClients(int channelID) {
        Collections.sort(getClientsByChannelID(channelID),new ClientOrderer(this));
    }

    public void setClientID(Integer _ClientID, Integer _ChannelID) {
    clientID = _ClientID;
    }

    public void ClientListReceived() {
        ClientsReceived = true;
    }

    public void ChannelsReceived() {
        ChannelsReceived = true;
    }

    public void UpdateConnectionInfo(String response) {
        Log.w("ServerConnection","UpdateConnectionInfo response received :"+response);
    }

    public void addRequest(ItemNotFoundType type) {
        if (!Requests.contains(type)) {
            Requests.add(type);
        }

    }

    public List<ItemNotFoundType> getRequests() {
        if ((Name == null) || (Name.equals("Unknown server"))) {
            Requests.add(ItemNotFoundType.ServerInfo);
        }
        return Requests;
    }

    public void clearRequests() {
        Requests.clear();
    }

    public String getName() {
        return Name;
    }
    public static class SCException extends Exception {
        private final String errorDescription;
        private final SCExceptionType type;
        public SCException(String message, SCExceptionType _type){
            super(message);
            this.type = _type;
            this.errorDescription = message;
        }
        public SCException(String message, SCExceptionType _type, Throwable tr){
            super(message,tr);
            this.type = _type;
            this.errorDescription = message;
        }
        public String getErrorDescription(){
            return errorDescription;
        }
        public SCExceptionType getType(){
            return type;
        }

    }
    public static class SCNotFoundException extends SCException {
        private final String errorDescription;
        private final ItemNotFoundType itemNotFoundType;
        public SCNotFoundException(String message, ItemNotFoundType _ItemNotFoundType){
            super(message,SCExceptionType.ItemNotFound);
            this.errorDescription = message;
            this.itemNotFoundType = _ItemNotFoundType;
        }
        public SCNotFoundException(String message,ItemNotFoundType _ItemNotFoundType, Throwable tr)
        {super(message,SCExceptionType.ItemNotFound,tr);
            this.errorDescription = message;
            this.itemNotFoundType = _ItemNotFoundType;
        }
        public String getErrorDescription(){
            return errorDescription;
        }
        public ItemNotFoundType getItemNotFoundType(){
            return itemNotFoundType;
        }
        public SCExceptionType getType(){
            return SCExceptionType.ItemNotFound;
        }
    }

    public enum ItemNotFoundType {
        Client,
        Channel,
        ChannelGroup,
        ServerGroup,
        ServerInfo
    }

    public enum SCExceptionType {
        ItemNotFound,
        DataBlockParseFailed

    }
}