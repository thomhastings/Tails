/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Zoe Lee (Kihira) and iLexiconn
 *
 * See LICENSE for full License
 */

package kihira.tails.common;

import net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.versioning.VersionParser;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.StatCollector;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URL;

public class FoxLibManager {

    public static final String foxlibVersion = "@FOXLIBVERSION@";
    public static final String foxlibReqVersion = "[1.7.10-0.6.0,)";
    public static final String foxlibFileName = "FoxLib-"+foxlibVersion+".jar";
    public static final String foxlibDownloadLink = "http://maven.kihirakreations.co.uk/kihira/FoxLib/"+foxlibVersion+"/"+foxlibFileName;
    public static final String foxlibDownloadFallback = "http://minecraft.curseforge.com/mc-mods/223291-foxlib/files";
    public static final Logger logger = LogManager.getLogger("FoxLib Manager");

    @SidedProxy(serverSide = "kihira.tails.common.FoxLibManager$CommonProxy", clientSide = "kihira.tails.common.FoxLibManager$ClientProxy")
    public static CommonProxy proxy;
    public static boolean outdated;
    long totalSize;

    /**
     * Checks if FoxLib is available and updated to the required version
     * @return If FoxLib is available
     */
    public static boolean checkFoxlib() {
        if (!isFoxlibInstalled()) {
            logger.error("FoxLib is not installed!");
            if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
                FMLCommonHandler.instance().bus().register(new FoxLibManager());
            }
            else {
                String s = StatCollector.translateToLocalFormatted("foxlib.downloader.missing", Tails.MOD_ID, foxlibDownloadFallback);
                FMLLog.bigWarning(s);
                FMLCommonHandler.instance().getSidedDelegate().haltGame(s, null);
            }
        }
        else if (!isFoxlibCorrectVersion()) {
            logger.error("FoxLib is not the correct version! Expected " + foxlibVersion + " got " + Loader.instance().getIndexedModList().get("foxlib").getDisplayVersion());
            if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
                outdated = true;
                FMLCommonHandler.instance().bus().register(new FoxLibManager());
            }
            else {
                String s = StatCollector.translateToLocalFormatted("foxlib.downloader.outofdate", foxlibDownloadFallback);
                FMLLog.bigWarning(s);
                FMLCommonHandler.instance().getSidedDelegate().haltGame(s, null);
            }
        }
        else {
            return true;
        }
        return false;
    }

    public static boolean isFoxlibInstalled() {
        return Loader.isModLoaded("foxlib");
    }

    public static boolean isFoxlibCorrectVersion() {
        try {
            //If we are in dev, skip version check
            byte[] bs = Launch.classLoader.getClassBytes("net.minecraft.world.World");
            if (bs != null) {
                logger.warn("We are in a dev environment, skipping version check");
                return true;
            }
        }
        catch (IOException ignored) { }

        System.out.println(Loader.instance().getIndexedModList().get("foxlib").getProcessedVersion().getVersionString());

        return VersionParser.parseRange(foxlibReqVersion).containsVersion(Loader.instance().getIndexedModList().get("foxlib").getProcessedVersion());
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
                @Override
                public void confirmClicked(boolean yesButton, int screenID) {
                    if (yesButton) {
                        if (outdated) deleteOldVersions();
                        downloadFoxlib();
                    }
                    else {
                        if (outdated) {
                            proxy.throwFoxlibError();
                        }
                        else {
                            FMLLog.bigWarning("foxlib.downloader.missing", foxlibDownloadFallback);
                        }
                        Minecraft.getMinecraft().shutdown();
                    }
                }
            }, outdated ? I18n.format("foxlib.downloader.2", Tails.MOD_ID) : I18n.format("foxlib.downloader.0", Tails.MOD_ID),
               outdated ? I18n.format("foxlib.downloader.3", Tails.MOD_ID) : I18n.format("foxlib.downloader.1"), 0));
        }
    }

    @SideOnly(Side.CLIENT)
    public void downloadFoxlib() {
        final GuiScreenWorking screenWorking = new GuiScreenWorking();
        Minecraft.getMinecraft().displayGuiScreen(screenWorking);

        final Thread downloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                screenWorking.resetProgressAndMessage(I18n.format("foxlib.downloader.downloading"));
                //screenWorking.resetProgresAndWorkingMessage("Starting..."); TODO

                File target;
                URL download;
                OutputStream output = null;
                InputStream input = null;
                try {
                    target = new File(Minecraft.getMinecraft().mcDataDir + File.separator + "mods" + File.separator + foxlibFileName);
                    download = new URL(foxlibDownloadLink);
                    output = new FileOutputStream(target);
                    input = download.openStream();
                    DownloadCountingOutputStream countingOutputStream = new DownloadCountingOutputStream(output, screenWorking);

                    totalSize = Long.valueOf(download.openConnection().getHeaderField("Content-Length"));
                    screenWorking.displayLoadingString(String.format("Downloading file (%.3f MB)...", totalSize / 1000000F));

                    IOUtils.copy(input, countingOutputStream);
                } catch (IOException e) {
                    //Delete file on close cause it could be corrupt
                    new File(Minecraft.getMinecraft().mcDataDir + File.separator + "mods" + File.separator + foxlibFileName).deleteOnExit();
                    e.printStackTrace();

                    proxy.showFailedScreen();
                } finally {
                    IOUtils.closeQuietly(output);
                    IOUtils.closeQuietly(input);
                }
            }
        }, "FoxLib Downloader");
        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    private void deleteOldVersions() {
        File modsFolder = new File(Minecraft.getMinecraft().mcDataDir + File.separator + "mods");
        File[] files = modsFolder.listFiles((FileFilter) new WildcardFileFilter("*FoxLib*.jar", IOCase.INSENSITIVE));
        if (files != null) {
            for (File file : files) {
                if (file.exists()) {
                    logger.info("Deleting file " + file.toString() + " on exit");
                    file.deleteOnExit();
                }
            }
        }
    }

    private class DownloadCountingOutputStream extends CountingOutputStream {

        private final IProgressUpdate update;

        public DownloadCountingOutputStream(OutputStream out, IProgressUpdate update) {
            super(out);
            this.update = update;
        }

        @Override
        protected void afterWrite(int n) throws IOException {
            super.afterWrite(n);

            if (getByteCount() == totalSize) {
                proxy.showRestartScreen();
            }

            update.setLoadingProgress((int) (getByteCount() / totalSize));
        }
    }

    @SideOnly(Side.CLIENT)
    private static class GuiScreenHold extends GuiScreen {
        private String topMessage;
        private String bottomMessage;

        public GuiScreenHold(String topMessage, String bottomMessage) {
            this.topMessage = topMessage;
            this.bottomMessage = bottomMessage;
        }

        public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, topMessage, width / 2, 90, 16777215);
            drawCenteredString(fontRendererObj, bottomMessage, width / 2, 110, 16777215);
        }

        protected void keyTyped(char character, int keycode) {}
    }

    public static class CommonProxy {
        public void throwFoxlibError() {
            FMLLog.bigWarning(StatCollector.translateToLocalFormatted("foxlib.downloader.outofdate", foxlibDownloadFallback));
        }

        public void showRestartScreen() {}
        public void showFailedScreen(){}
    }

    @SideOnly(Side.CLIENT)
    public static class ClientProxy extends CommonProxy {
        public void throwFoxlibError() {
            throw new CustomModLoadingErrorDisplayException() {
                @Override
                public void initGui(GuiErrorScreen errorScreen, FontRenderer fontRendererObj) {
                    try {
                        Desktop.getDesktop().browse(URI.create(foxlibDownloadFallback));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void drawScreen(GuiErrorScreen errorScreen, FontRenderer fontRendererObj, int mouseRelX, int mouseRelY, float tickTime) {
                    String s = I18n.format("foxlib.downloader.outofdate");
                    fontRendererObj.drawString(s, (errorScreen.width / 2) - (fontRendererObj.getStringWidth(s) / 2), errorScreen.height / 2, 0xFFFFFFFF);
                }
            };
        }

        public void showRestartScreen() {
            Minecraft.getMinecraft().displayGuiScreen(new GuiScreenHold(I18n.format("foxlib.downloader.success"), I18n.format("foxlib.downloader.restart")));
        }

        public void showFailedScreen() {
            try {
                Desktop.getDesktop().browse(URI.create(foxlibDownloadFallback));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Minecraft.getMinecraft().displayGuiScreen(new GuiScreenHold(I18n.format("foxlib.downloader.failed.0", foxlibDownloadFallback), I18n.format("foxlib.downloader.failed.1")));
        }
    }
}