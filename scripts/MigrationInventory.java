import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.io.*;

/** Read-only capture/compare of every original field. Run while application writes are paused. */
class MigrationInventory {
    record Table(String name,List<String> columns,long count,String hash) {}
    static String quote(String name){return "`"+name.replace("`","``")+"`";}
    static String b64(String value){return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    static String decode(String value){return new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8);}
    static Table capture(Connection c,String table,List<String> columns)throws Exception {
        List<String> keys=new ArrayList<>();try(var pk=c.getMetaData().getPrimaryKeys(c.getCatalog(),null,table)){TreeMap<Short,String> order=new TreeMap<>();while(pk.next())order.put(pk.getShort("KEY_SEQ"),pk.getString("COLUMN_NAME"));keys.addAll(order.values());}
        if(keys.isEmpty())throw new IllegalStateException("Table requires a stable primary key: "+table);
        String sql="SELECT "+String.join(",",columns.stream().map(MigrationInventory::quote).toList())+" FROM "+quote(table)+" ORDER BY "+String.join(",",keys.stream().map(MigrationInventory::quote).toList());
        MessageDigest hash=MessageDigest.getInstance("SHA-256");long count=0;
        try(var statement=c.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY)){statement.setFetchSize(Integer.MIN_VALUE);try(var rows=statement.executeQuery(sql)){while(rows.next()){count++;for(int n=1;n<=columns.size();n++){byte[] bytes=rows.getBytes(n);hash.update((byte)(bytes==null?0:1));if(bytes!=null){int length=bytes.length;hash.update(new byte[]{(byte)(length>>>24),(byte)(length>>>16),(byte)(length>>>8),(byte)length});hash.update(bytes);}}}}}
        return new Table(table,columns,count,HexFormat.of().formatHex(hash.digest()));
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=2||!Set.of("capture","compare").contains(args[0]))throw new IllegalArgumentException("Usage: capture|compare inventory.tsv; DB_URL/DB_USER/DB_PASSWORD required");
        String url=System.getenv("DB_URL"),user=System.getenv("DB_USER"),password=System.getenv("DB_PASSWORD");if(url==null||user==null||password==null)throw new IllegalArgumentException("Explicit database credentials required");
        Path file=Path.of(args[1]);try(Connection c=DriverManager.getConnection(url,user,password)){
            c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
            if(args[0].equals("capture")){
                if(Files.exists(file))throw new IllegalStateException("Inventory already exists; choose a new path");List<Table> tables=new ArrayList<>();List<String> names=new ArrayList<>();try(var list=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})){while(list.next()){String name=list.getString("TABLE_NAME");if(!name.equals("flyway_schema_history"))names.add(name);}}Collections.sort(names);
                for(String name:names){List<String> columns=new ArrayList<>();try(var list=c.getMetaData().getColumns(c.getCatalog(),null,name,"%")){while(list.next())columns.add(list.getString("COLUMN_NAME"));}tables.add(capture(c,name,columns));}
                List<String> lines=new ArrayList<>(List.of("# inventory-v1: table\tbase64-columns\tcount\tsha256"));for(Table t:tables)lines.add(t.name()+"\t"+b64(String.join(",",t.columns()))+"\t"+t.count()+"\t"+t.hash());Files.write(file,lines,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);System.out.println("Captured "+tables.size()+" tables; no record values exported.");
            }else{
                int verified=0;for(String line:Files.readAllLines(file)){if(line.startsWith("#"))continue;String[] fields=line.split("\t");Table actual=capture(c,fields[0],List.of(decode(fields[1]).split(",")));if(actual.count()!=Long.parseLong(fields[2])||!actual.hash().equals(fields[3]))throw new IllegalStateException("Original fields changed: "+fields[0]);verified++;}System.out.println("Verified "+verified+" original tables, record counts and all original fields unchanged.");
            }c.rollback();
        }
    }
}
