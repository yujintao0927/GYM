package com.gym.modules.rankingcore.persistence;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import com.gym.modules.rankingcore.api.Failure;
public final class Database  {
    private final String url,user,password;
    private Database(String u,String n,String p) {
        url=u;
        user=n;
        password=p;
    }
    public static Database open(String url,String user,String password) {
        if(url==null||!url.equals("jdbc:mysql://localhost:3306/gym_dev_test_biz006"))throw new IllegalArgumentException("Explicit BIZ006 URL required");
        var db=new Database(url,user,password);
        db.transaction(c-> {
            if(!"gym_dev_test_biz006".equals(c.getCatalog()))throw new Failure(503,"DATABASE_MISMATCH");
            try(var r=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[] {
                "TABLE"
            }
            )) {
                while(r.next())if(!Set.of("flyway_schema_history","vis_lock","vis_visit","vis_challenge","rnk_receipt","rnk_outbox","rnk_raw","rnk_day","rnk_participant","rnk_appeal").contains(r.getString("TABLE_NAME")))throw new Failure(503,"UNKNOWN_TABLE_REQUIRES_REVIEW");
            }
            return null;
        }
        );
        db.migrate();
        return db;
    }
    public int migrate() {
        return Flyway.configure().dataSource(url,user,password).locations("classpath:db/modules/ranking-core").cleanDisabled(true).baselineOnMigrate(false).load().migrate().migrationsExecuted;
    }
    @FunctionalInterface public interface Work<T> {
        T run(Connection c)throws Exception;
    }
    public <T>T transaction(Work<T> work) {
        try(var c=DriverManager.getConnection(url,user,password)) {
            c.setAutoCommit(false);
            try {
                T result=work.run(c);
                c.commit();
                return result;
            }
            catch(Exception e) {
                c.rollback();
                if(e instanceof Failure f)throw f;
                throw new Failure(503,"PERSISTENCE_UNAVAILABLE");
            }
        }
        catch(SQLException e) {
            throw new Failure(503,"PERSISTENCE_UNAVAILABLE");
        }
    }
    public static void execute(Connection c,String sql,Object... args)throws SQLException {
        try(var q=c.prepareStatement(sql)) {
            bind(q,args);
            q.executeUpdate();
        }
    }
    public static void bind(PreparedStatement q,Object... args)throws SQLException {
        for(int i=0;i<args.length;i++)q.setObject(i+1,args[i]);
    }
    public static String scalar(Connection c,String sql,Object... args)throws SQLException {
        try(var q=c.prepareStatement(sql)) {
            bind(q,args);
            try(var r=q.executeQuery()) {
                return r.next()?r.getString(1):null;
            }
        }
    }
    public static void lock(Connection c,String key)throws SQLException {
        execute(c,"INSERT INTO vis_lock(lock_key) VALUES (?) ON DUPLICATE KEY UPDATE lock_key=lock_key",key);
        scalar(c,"SELECT lock_key FROM vis_lock WHERE lock_key=? FOR UPDATE",key);
    }
}
