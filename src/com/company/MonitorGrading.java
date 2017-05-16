package com.company;

import com.sun.xml.internal.org.jvnet.mimepull.MIMEMessage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

public class MonitorGrading {
    private static int maxNum;
    public static String myEmailAccount = "523213189@qq.com";
    public static String myEmailPassword = "j2mv9jyyq6j";
    public static String myEmailSMTPHost = "smtp.qq.com";
    public static String receiveMailAccount = "523213189@qq.com";

    public static void main(String[] args) {
        String username;
        String password;
        if (args.length == 0){
            username = "2015300955";
            password = "J2mv9jyyq6";
        }
        else{
            username = args[0];
            password = args[1];
            receiveMailAccount = args[2];
        }
        String param = "username=" + username + "&password=" + password;

        new Thread(() -> {
            while(true){
                try {
                    URL url = new URL("http://us.nwpu.edu.cn/eams/login.action");

                    //获取Cookie
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    String cookie = connection.getHeaderField("Set-Cookie");


                    //模拟登陆
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Cookie", cookie);

                    OutputStream os = connection.getOutputStream();
                    os.write(param.getBytes("GBK"));
                    os.close();

                    //并不知道为什么加这个，但是不加这个下面就无法正常获取数据
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    br.close();


                    //获取成绩
                    url = new URL("http://us.nwpu.edu.cn/eams/teach/grade/course/person!historyCourseGrade.action?projectType=MAJOR");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("Cookie", cookie);

                    //获取网页源代码
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));
                    StringBuilder builder = new StringBuilder();

                    String line = "";
                    while ((line = reader.readLine()) != null){
                        builder.append(line + '\n');
                    }
                    reader.close();

                    //利用JSoup解析
                    Document document = Jsoup.parse(builder.toString());
                    Elements tableTags = document.select("table");
                    int total = 0;
                    for (Element tableTag : tableTags){
                        if (tableTag.attr("class").equals("gridtable")){
                            Elements trTags = tableTag.select("tr");
                            for (Element trTag : trTags){
                                Elements thTags = trTag.select("th");
                                if (thTags.size() < 4){
                                    continue;
                                }
                                if (thTags.get(0).text().equals("School Summary")) {
                                    total = Integer.parseInt(thTags.get(1).text());
                                    break;
                                }
                            }
                        }
                    }
                    if (total > maxNum){
                        int preMaxNum = maxNum;
                        maxNum = total;
                        if (preMaxNum == 0){
                            continue;
                        }
                        new Thread(() -> {
                            System.out.println("我是子线程");

                            Properties properties = new Properties();
                            properties.setProperty("mail.transport.protocol", "smtp");   // 使用的协议（JavaMail规范要求）
                            properties.setProperty("mail.smtp.host", myEmailSMTPHost);   // 发件人的邮箱的 SMTP 服务器地址
                            properties.setProperty("mail.smtp.auth", "true");            // 需要请求认证
                            final String smtpPort = "465";
                            properties.setProperty("mail.smtp.port", smtpPort);
                            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                            properties.setProperty("mail.smtp.socketFactory.fallback", "false");
                            properties.setProperty("mail.smtp.socketFactory.port", smtpPort);
                            Session session = Session.getDefaultInstance(properties);
                            session.setDebug(true);
                            MimeMessage message;
                            try {
                                message = createMimeMessage(session, myEmailAccount, receiveMailAccount);
                                Transport transport = session.getTransport();
                                transport.connect(myEmailAccount, myEmailPassword);
                                transport.sendMessage(message, message.getAllRecipients());
                                transport.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }).start();
                    }
                    System.out.println(maxNum);
                    Thread.sleep(10000);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    public static MimeMessage createMimeMessage(Session session, String sendMail, String receiveMail) throws Exception {
        // 1. 创建一封邮件
        MimeMessage message = new MimeMessage(session);

        // 2. From: 发件人
        message.setFrom(new InternetAddress(sendMail, "doufu", "UTF-8"));

        // 3. To: 收件人（可以增加多个收件人、抄送、密送）
        message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(receiveMail, "zhang", "UTF-8"));

        // 4. Subject: 邮件主题
        message.setSubject("出成绩啦！！！", "UTF-8");

        // 5. Content: 邮件正文（可以使用html标签）
        message.setContent("出成绩啦！请查询", "text/html;charset=UTF-8");

        // 6. 设置发件时间
        message.setSentDate(new Date());

        // 7. 保存设置
        message.saveChanges();

        return message;
    }
}
