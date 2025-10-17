package ths;

public class AllInOneLauncher {

    public static void main(String[] args) throws Exception {
        Thread server = new Thread(() -> {
            try {
                ths.server.ServerMain.main(new String[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "ths-server");
        server.setDaemon(true);
        server.start();
        Thread.sleep(Integer.getInteger("SERVER_BOOT_WAIT", 2500));
        ths.client.App.main(args);

        // wait so the server binds the port
        Thread.sleep(Integer.getInteger("SERVER_BOOT_WAIT", 2500));

        // now start the client
        ths.client.App.main(args);

    }
}
