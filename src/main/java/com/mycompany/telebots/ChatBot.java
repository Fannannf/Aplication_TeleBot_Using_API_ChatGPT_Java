/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.telebots;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JOptionPane;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import okhttp3.*;
/**
 *
 * @author HP
 */
public class ChatBot extends TelegramLongPollingBot {
    private static final String OPENAI_API_KEY = "YOUR_TOKEN_APIGPT";
    private static final String BOT_TOKEN = "YOUR_TOKEN_TELEBOT";
    private static final String DB_URL = "YOUR_URL_DATABASE";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "";
    
    String tanggal;
    SimpleDateFormat a;
    ResultSet Rs;
    @Override
    public void onUpdateReceived(Update update) {
        format_tanggal();
        SendMessage message = new SendMessage();
        User user = update.getMessage().getFrom();
        try(Connection connection = createConnection()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if(messageText.equals("/start")){
               sendMessage(chatId,"Welcome to ChatBot :)");
               saveMessageToDatabase(chatId,tanggal,user.getFirstName(),messageText);
            }else if(messageText.equals("/register")){
                String sql = "SELECT COUNT(*) FROM users WHERE chat_id = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setLong(1, chatId);
                ResultSet resultSet = statement.executeQuery();

                // Mendapatkan jumlah baris yang sesuai dengan chatId
                resultSet.next();
                int count = resultSet.getInt(1);

                if (count > 0) {
                    // chatId sudah terdaftar dalam database
                    // Lakukan logika yang sesuai
                    sendMessage(chatId,"Sorry, your account is already registered");
                    sendMessage(chatId,"Please type /help for help if you have trouble using this bot!");
                }else{
                    String name = user.getFirstName();
                    registerUser(chatId,name);
                    sendMessage(chatId,"Thank you for registering, you can use the keyword /help to get help :)");
                }
                saveMessageToDatabase(chatId,tanggal,user.getFirstName(),messageText);
                resultSet.close();
                statement.close();
            }else{
                String sql = "SELECT COUNT(*) FROM users WHERE chat_id = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setLong(1, chatId);
                ResultSet resultSet = statement.executeQuery();

                // Mendapatkan jumlah baris yang sesuai dengan chatId
                resultSet.next();
                int count = resultSet.getInt(1);
                if (count > 0) {
                    String pesan = update.getMessage().getText();
                    if(pesan.equals("/name")){
                        sendMessage(chatId,"Halloo "+ user.getFirstName() +" How Are You?");
                    }else if(pesan.equals("/help")){
                        Statement stm = connection.createStatement();
                        Rs = stm.executeQuery("SELECT * FROM command");
                        StringBuilder result = new StringBuilder();
                        while(Rs.next()){
                            String commandMessage = Rs.getString("keyword");
                            String deskirpsi = Rs.getString("deskripsi");
                            result.append(commandMessage +" - "+deskirpsi).append("\n");
                        }
                        sendMessage(chatId,"Some keywords that can be used : \n\n"+result.toString()+"\nyou can use this command\n\nThanks for asking for our help :)");
                        saveMessageToDatabase(chatId,tanggal,user.getFirstName(),messageText);
                        Rs.close();
                        stm.close();
                    }else {
                        String sql2 = "SELECT * FROM command WHERE keyword = ?";
                        PreparedStatement statement2 = connection.prepareStatement(sql2);
                        statement2.setString(1, pesan);
                        ResultSet resultSet2 = statement2.executeQuery();

                        if (resultSet2.isBeforeFirst()) {
                            resultSet2.next();
                            int count1 = resultSet2.getInt(1);
                            if (count1 > 0) {
                                String pesanDB = resultSet2.getString("keyword");
                                String jawabanDB = resultSet2.getString("jawaban");
                                if (pesan.equals(pesanDB)) {
                                    sendMessage(chatId, jawabanDB);
                                } else {
                                    generateGPT3Response(chatId,pesan);
                                }
                            } else {
                                generateGPT3Response(chatId,pesan);
                            }
                        } else {
                            generateGPT3Response(chatId,pesan);
                        }
                        //sendMessage(chatId,pesan);
                        saveMessageToDatabase(chatId,tanggal,user.getFirstName(),messageText);
                        resultSet2.close();
                        statement2.close();
                    }
                } else {
                    // chatId belum terdaftar dalam database
                    // Lakukan logika yang sesuai
                    sendMessage(chatId,"Sorry, you must / register first");
                }
                saveMessageToDatabase(chatId,tanggal,user.getFirstName(),messageText);
                // Tutup statement dan resultSet
                resultSet.close();
                statement.close();
            }
        }catch(SQLException e){
            JOptionPane.showMessageDialog(null, e);
        }
    }
    
    private void format_tanggal()
    {
        String DATE_FORMAT = "yyyy-MM-dd";
        java.text.SimpleDateFormat sdf = new
        java.text.SimpleDateFormat(DATE_FORMAT);
        Calendar c1 = Calendar.getInstance();
        int year=c1.get(Calendar.YEAR);
        int month=c1.get(Calendar.MONTH)+1;
        int day=c1.get(Calendar.DAY_OF_MONTH);
        tanggal=Integer.toString(year)+"-"+Integer.toString(month)+"-"+Integer.toString(day);
    }
    
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }
    
    void sendMessage(long chatId, String messagee) {
        format_tanggal();
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messagee);
        String pesanlist = "pesan dari "+chatId+" : "+messagee;
        try {
            execute(message);
            saveMessageToDatabase(chatId,tanggal, "telebot", messagee);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void registerUser(long chatId, String username) {
        try (Connection connection = createConnection()) {
            try{
                String sql = "INSERT INTO users (chat_id, nama) VALUES (" + chatId + ", '" + username + "')";
                Statement statement;
                statement = connection.createStatement();
                statement.executeUpdate(sql);
                sendMessage(chatId,"Your account has been added successfully");
                sendMessage(chatId,"Your Name : "+username);
            }catch (SQLException e){
                e.printStackTrace();
                sendMessage(chatId, "Failed to register user. Please try again later.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Failed to register user. Please try again later.");
        }
    }
    
    private void saveMessageToDatabase(long chatId,String tgl, String username, String messageText) {
        try (Connection connection = createConnection()) {
            try {
                String sql = "INSERT INTO history (tgl_pesan,nama, pesan) VALUES ('" + tgl + "','" + username + "', '" + messageText + "')";
                Statement statement;
                statement = connection.createStatement();
                statement.executeUpdate(sql);
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void generateGPT3Response(long chatid,String inputText){
        try {
            URL url = new URL("https://api.openai.com/v1/engines/gpt-3.5-turbo/completions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
            connection.setDoOutput(true);

            String body = "{\"messages\": [{\"role\":\"system\", \"content\":\"You are a helpful assistant.\"}, {\"role\":\"user\", \"content\":\"" + inputText + "\"}]}";
            OutputStreamWriter write = new OutputStreamWriter(connection.getOutputStream());
            write.write(body);
            write.flush();
            write.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String responseBody = response.toString();
            String pesan = responseBody.split("\"text\":\"")[1].split("\"")[0];
            sendMessage(chatid,pesan);
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatid,e.getMessage());
        }
    }




    @Override
    public String getBotUsername() {
        return "YOUR_USERNAME_BOT";
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }
    
    //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

    public static void main(String[] args) {
    }
}
