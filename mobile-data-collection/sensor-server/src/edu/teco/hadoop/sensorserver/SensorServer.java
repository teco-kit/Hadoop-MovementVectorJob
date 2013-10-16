package edu.teco.hadoop.sensorserver;
 
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import java.io.*;
 
public class SensorServer {
 
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(SensorServlet.class, "/sensorserver");
        server.start();
        server.join();
    }
 
    public static class SensorServlet extends HttpServlet {
 
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

            response.addHeader("Access-Control-Allow-Origin", "*");
            BufferedReader reader = request.getReader();
            StringBuffer sb = new StringBuffer();
            String line = reader.readLine();
            while(line!=null)
            {
                sb.append(java.net.URLDecoder.decode(line.split("=")[1],"UTF-8"));
                sb.append("\n");
                line = reader.readLine();
            }


            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/tmp/sensoroutput.txt",true)));
            out.print(sb.toString());
            out.close();

            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_OK);

            LineNumberReader  lnr = new LineNumberReader(new FileReader(new File("/tmp/sensoroutput.txt")));
            lnr.skip(Long.MAX_VALUE);

            response.getWriter().println(lnr.getLineNumber()+1);
        }
    }
}