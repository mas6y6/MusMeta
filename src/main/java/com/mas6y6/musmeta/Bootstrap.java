package com.mas6y6.musmeta;

import com.mas6y6.musmeta.utils.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.logging.Level;

@Command(
        name = "musmeta",
        mixinStandardHelpOptions = true,
        versionProvider = Bootstrap.VersionProvider.class,
        description = "An application for editing music file metadata."
)
public class Bootstrap implements Runnable {

    public static final Logger LOGGER =
            LoggerFactory.getLogger(Bootstrap.class);

    @CommandLine.Option(
            names = {"--debug","-d"},
            description = "Enable debug logging."
    )
    private boolean debug;

    @CommandLine.Option(
            names = {"--skip-bootstrap","-b"},
            description = "Skip MusMeta bootstrap."
    )
    private boolean skipbootstrap;

    @Override
    public void run() {
        System.out.println("java.home = " + System.getProperty("java.home"));
        System.out.println("java.version = " + System.getProperty("java.version"));
        System.out.println("java.vendor = " + System.getProperty("java.vendor"));
        LOGGER.info("MusMeta - {}", Version.get());
        if (debug) {
            LOGGER.info("Running in debug mode");
            System.setProperty(
                    "org.slf4j.simpleLogger.defaultLogLevel",
                    "debug"
            );
        }

        if (!skipbootstrap) {
            LOGGER.info("Running MusMeta bootstrap...");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("MusMeta shutdown in progress...");
            // TODO: when plugin implementation is completed add shutdown hook here
            LOGGER.info("MusMeta is shutdown completed.");
        }));

        Main.main();
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler::handle);

        CommandLine commandLine = new CommandLine(new Bootstrap());

        commandLine
                .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                    CrashHandler.handle(
                            Thread.currentThread(),
                            ex
                    );

                    return 1;
                })
                .execute(args);
    }

    public static class VersionProvider
            implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            String version = Bootstrap.class
                    .getPackage()
                    .getImplementationVersion();

            if (version == null) {
                version = "development";
            }

            return new String[] {
                    "MusMeta " + version
            };
        }
    }
}