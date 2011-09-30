package com.ribomation.droidAtScreen.gui;

import com.ribomation.droidAtScreen.dev.AndroidDevice;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Renders the screen shots of a connected Android device.
 *
 * @user jens
 * @date 2010-jan-17 20:43:00
 */
public class DevicePane extends JPanel {
    private final RenderingHints    hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    private final Logger            log = Logger.getLogger(DevicePane.class);
    private BufferedImage       lastScreenShot;
    private DeviceFrame deviceFrame;
    private AndroidDevice       device;
    private Timer               timer;
    private int                 frameRate = 15, scalePercentage = 100;
    private boolean             portrait = true, upsideDown = true;
    private AffineTransformOp   transformOP = null;
    private AffineTransform     scaleTX, upsideDownTX;

    public DevicePane(DeviceFrame deviceFrame, AndroidDevice dev, boolean portrait, boolean upsideDown, int scalePercentage, int frameRate) {
        super(new BorderLayout(), true);
        this.deviceFrame = deviceFrame;
        this.device = dev;
        this.portrait = portrait;
        this.upsideDown = upsideDown;
        this.scalePercentage = scalePercentage;
        this.frameRate = frameRate;

        updateView();
//        setPortrait(portrait);
//        setScalePercentage(scalePercentage);
//        setFrameRate(frameRate);
    }

    public AndroidDevice getDevice() {
        return device;
    }

//    public void setPortrait(boolean portrait) {
//        this.portrait = portrait;
//        updateView();
//    }

//    public void setScalePercentage(int scalePercentage) {
//        if ((scalePercentage < 10) || (300 < scalePercentage)) {
//            throw new IllegalArgumentException("Scale % is invalid: " + scalePercentage);
//        }
//        this.scalePercentage = scalePercentage;
//        this.scaleTX = null;
//
////        if (scalePercentage != 100) {
////            double scale = scalePercentage / 100.0;
////            AffineTransform tx = AffineTransform.getScaleInstance(scale, scale);
////            transformOP = new AffineTransformOp(tx, hints);
////        } else {
////            transformOP = null;
////        }
//        updateView();
//    }

//    public void setFrameRate(int rate) {
//        if (rate <= 0) {
//            throw new IllegalArgumentException("Frame rate must be a positive number. Value=" + rate);
//        }
//        this.frameRate = rate;
//    }

    public BufferedImage getLastScreenShot() {
        return lastScreenShot;
    }

    public void setLastScreenShot(BufferedImage lastScreenShot) {
        this.lastScreenShot = lastScreenShot;
    }

    protected void paintComponent(Graphics g) {
        BufferedImage img = getLastScreenShot();
        if (img == null) {
            return;
        }

        int x = (this.getWidth() - img.getWidth()) / 2;
        int y = (this.getHeight() - img.getHeight()) / 2;
        g.drawImage(img, x, y, this);
    }

    private BufferedImage fetchScreenshot() {
        BufferedImage img = device.getScreenShot(!this.portrait);
        if (img == null) return null;
        
//        if (transformOP != null && img != null) {
//            img = transformOP.filter(img, null);
//        }

        if (scalePercentage != 100) {
            if (scaleTX == null) {
                double scale = scalePercentage / 100.0;
                scaleTX = AffineTransform.getScaleInstance(scale, scale);
            }
            img = new AffineTransformOp(scaleTX, hints).filter(img, null);
        }
        
        if (upsideDown) {
            if (upsideDownTX == null) {
                double x = img.getWidth() / 2;
                double y = img.getHeight() / 2;
                upsideDownTX = AffineTransform.getQuadrantRotateInstance(2, x, y);
            }
            img = new AffineTransformOp(upsideDownTX, hints).filter(img, null);
        }
        
        return img;
    }

    protected void updateView() {
        updateView(fetchScreenshot());
    }

    protected void updateView(BufferedImage img) {
        Dimension sz = new Dimension(img.getWidth(), img.getHeight());
        this.setMinimumSize(sz);
        this.setSize(sz);
        
        deviceFrame.setPreferredSize( add(sz, deviceFrame.getInsets()) );
        deviceFrame.pack();
    }

    protected Dimension add(Dimension sz, Insets pad) {
        return new Dimension(pad.left + sz.width + pad.right, pad.top + sz.height + pad.bottom);
    }

    public void     update() {
        setLastScreenShot( fetchScreenshot() );
        repaint();
    }

    public void start() {
        long updatePeriod = 1000L / frameRate;
        timer = new Timer("ScreenShot Updater");
        timer.scheduleAtFixedRate(new Updater(), 2*updatePeriod, updatePeriod);
    }

    public void  stop() {
        timer.cancel();
        timer = null;
    }

    class Updater extends TimerTask {
        private AtomicBoolean   busy = new AtomicBoolean(false);

        @Override
        public void run() {
            if (busy.getAndSet(true)) return;
            try {
                update();
            } catch (Exception e) {
                log.debug(e.getMessage(),e);
            }
            busy.set(false);
        }
    }

}