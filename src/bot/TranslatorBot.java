/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
/**
 *
 * @author bensoutendijk
 */
public class TranslatorBot{

    public static void main(String args[]) throws Exception{
        TranslatorBot bot = new TranslatorBot();
        bot.connect();
        bot.joinChannel("#irchacks");
        String line = null;
        while((line = bot.readLine()) != null){
            System.out.println(line);
        }
    }

    public final String DEFAULT_SOURCE = "en";
    public final String DEFAULT_TARGET = "es";

    public final String HELP_COPY = "To translate from English to Spanish: Send a message like '!translate <english phrase>' replacing <english phrase> with the text you want to translate.";

    class Request{
        String q, target, source, format;
                void setQ(String q){
            this.q = q;
        }
        String getQ(){
            return this.q;
        }
        void setTarget(String target){
            this.target = target;
        }
        String getTarget(){
            return this.target;
        }
        void setSource(String source){
            this.source = source;
        }
        String getSource(){
            return this.source;
        }
        void setFormat(String format){
            this.format = format;
        }
        String getFormat(){
            return this.format;
        }
    }
    class Response{
        private Data data;
        void setData(Data data){
            this.data = data;
        }
        Data getData(){
            return this.data;
        }
    }
    class Data{
        List<Translations> translations;
        void setTranslations(List<Translations> translations){
            this.translations = translations;
        }
        List<Translations> getTranslations(){
            return this.translations;
        }
    }
    class Translations{
        String translatedText;
        void setTranslatedText(String translatedText){
            this.translatedText = translatedText;
        }
        String getTranslatedText(){
            return this.translatedText;
        }
    }

    private URL url = new URL("https://translation.googleapis.com/language/translate/v2");

    HttpClient httpClient = HttpClientBuilder.create().build();

    private String server, channel, name;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    public TranslatorBot() throws Exception{
        server = "irc.freenode.net";
        channel = "#irchacks";
        name = "LasTraducciones";
        socket = new Socket(server, 6667);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void connect() throws Exception{

        writer.write("NICK " + name + "\r\n");
        writer.write("USER " + name + " 8 * :Soutendijk Translation Bot\r\n");
        writer.flush();

        String line = null;
        while ((line = reader.readLine()) != null){
            if (line.indexOf("004") >= 0){
//                We have logged in
                break;
            } else if (line.indexOf("433") >= 0){
                System.out.println("Nickname already in use");
                return;
            }
        }
    }

    public void joinChannel(String channel) throws Exception {
        writer.write("JOIN " + channel + "\r\n");
        writer.flush();
    }

    public String readLine() throws Exception {
        String line = null;
        if ((line = reader.readLine()) != null){
            if (line.startsWith("PING ")) {
//                We must respond to pings to prevent getting disconnect from the server
                writer.write("PONG " + line.substring(5) + "\r\n");
                writer.flush();
            } else {
//                Check if chat message is sent by user
                if (line.contains("PRIVMSG")){
                    int i = line.indexOf(":",1);
                    String message = line.substring(i+1);
                    onMessage(message);
                }
            }
        } else {
            throw new Exception();
        }
        return line;
    }

    public void onMessage(String line) throws Exception{
//        Parse the message for applicable commands
        if (line.startsWith("!translate ")){
            String source = DEFAULT_SOURCE;
            String target = DEFAULT_TARGET;
            String arg = "";
            int i = line.indexOf(" ");
            if (line.substring(i+1).toLowerCase().startsWith("-help")){
                sendMessage(HELP_COPY);
                arg = "help";
            }
//            else if (line.charAt(i+1) == '-'){
//                arg = line.substring(i+2, i+7);
//                source = line.substring(i+2,i+4);
//                target = line.substring(i+5,i+7);
//            }
            String phrase = "";
            if (arg.isEmpty()){
                phrase = line.substring(i+1);
                String q = getTranslation(phrase, source, target);
                sendMessage(q);
            }
        }
    }

    private String getTranslation(String q, String source, String target) throws Exception{
        String translation = null;

        Gson gson = new Gson();

        Request request = new Request();
        request.setQ(q);
        request.setSource(source);
        request.setTarget(target);
        request.setFormat("text");
        StringEntity body = new StringEntity(gson.toJson(request));

        HttpPost post = new HttpPost(url.toString());
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Authorization", "Bearer " + getAuthToken());
        post.setEntity(body);
        HttpResponse httpResponse = httpClient.execute(post);
        Scanner s = new Scanner(httpResponse.getEntity().getContent()).useDelimiter("\\A");
        String jsonResponse = s.hasNext() ? s.next() : "";

        Response response = gson.fromJson(jsonResponse, Response.class);
        translation = response.getData().getTranslations().get(0).getTranslatedText();

        return translation;
    }

    private void sendMessage(String message) throws Exception{
        writer.write("PRIVMSG " + channel + " :" + message + "\r\n");
        writer.flush();
    }
    private String getAuthToken() throws Exception{
        Process p = new ProcessBuilder("/Users/username/Documents/.../google-cloud-sdk/bin/gcloud", "auth", "print-access-token").start();
        Scanner s = new Scanner(p.getInputStream()).useDelimiter("\\A");
        String res = s.hasNext() ? s.next() : "";
        return res;
    }

}
