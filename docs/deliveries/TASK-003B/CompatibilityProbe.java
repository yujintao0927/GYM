import java.sql.*;
import org.flywaydb.core.Flyway;
public class CompatibilityProbe {
 public static void main(String[] args) throws Exception {
  String url="jdbc:mysql://localhost:3306/gym_dev";
  String password=System.getenv("GYM_DB_PASSWORD");
  try(var c=DriverManager.getConnection(url,"root",password); var s=c.createStatement();var r=s.executeQuery("SELECT VERSION()")){r.next();System.out.println("JDBC_VERSION="+r.getString(1));}
  var f=Flyway.configure().dataSource(url,"root",password).cleanDisabled(true).load();
  f.info();
  System.out.println("FLYWAY_INFO_OK");
 }
}
