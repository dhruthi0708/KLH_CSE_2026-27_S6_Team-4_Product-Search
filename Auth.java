import java.io.*;

/* Auth - users.csv + session.txt (plain files, no database) */
class Auth {
    static final String USERS = "data/users.csv";
    static final String SESSION = "data/session.txt";

    static boolean register(String user, String pass) throws IOException {
        if (exists(user)) return false;
        PrintWriter pw = new PrintWriter(new FileWriter(USERS, true));
        pw.println(user + "," + hash(pass));
        pw.close();
        return true;
    }

    static boolean login(String user, String pass) throws IOException {
        File f = new File(USERS);
        if (!f.exists()) return false;
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line; boolean ok = false;
        String h = hash(pass);
        while ((line = br.readLine()) != null) {
            int c = line.indexOf(',');
            if (c < 0) continue;
            if (line.substring(0, c).equals(user) && line.substring(c + 1).equals(h)) { ok = true; break; }
        }
        br.close();
        return ok;
    }

    static boolean exists(String user) throws IOException {
        File f = new File(USERS);
        if (!f.exists()) return false;
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line; boolean found = false;
        while ((line = br.readLine()) != null) {
            int c = line.indexOf(',');
            if (c > 0 && line.substring(0, c).equals(user)) { found = true; break; }
        }
        br.close();
        return found;
    }

    /* simple polynomial hash so plain passwords are not stored */
    static String hash(String p) {
        long h = 7;
        for (int i = 0; i < p.length(); i++) h = (h * 131 + p.charAt(i)) % 1000000007L;
        return "" + h;
    }

    static void saveSession(String user) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(SESSION));
        pw.println("username=" + user);
        pw.println("loginTime=" + System.currentTimeMillis());
        pw.println("status=ACTIVE");
        pw.close();
    }

    static String readSession() throws IOException {
        File f = new File(SESSION);
        if (!f.exists()) return null;
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line, user = null; boolean active = false;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("username=")) user = line.substring(9);
            if (line.equals("status=ACTIVE")) active = true;
        }
        br.close();
        return active ? user : null;
    }

    static void clearSession() throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(SESSION));
        pw.println("status=LOGGED_OUT");
        pw.close();
    }
}
