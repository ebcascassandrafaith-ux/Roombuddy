package com.template.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

/** Loads listing photos from a local path or a URL, without ever throwing. */
public final class Images {

    private Images() {}

    /** @return the image, or null if the path is empty or unreadable */
    public static Image load(String path, double width, double height) {
        if (path == null || path.trim().isEmpty()) return null;
        try {
            String lower = path.toLowerCase();
            String url;
            if (lower.startsWith("http:") || lower.startsWith("https:") || lower.startsWith("file:")) {
                url = path;
            } else {
                File f = new File(path);
                if (!f.exists()) return null;
                url = f.toURI().toString();
            }
            Image img = new Image(url, width, height, true, true, true);
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }

    /** Shows the image as a centered square crop so it never looks stretched. */
    public static void showSquareCrop(ImageView view, Image img) {
        view.setImage(img);
        Runnable crop = () -> {
            double w = img.getWidth();
            double h = img.getHeight();
            if (w <= 0 || h <= 0) return;
            double side = Math.min(w, h);
            view.setViewport(new Rectangle2D((w - side) / 2, (h - side) / 2, side, side));
        };
        if (img.getProgress() >= 1.0) {
            crop.run();
        } else {
            img.progressProperty().addListener((obs, was, now) -> {
                if (now.doubleValue() >= 1.0) crop.run();
            });
        }
    }

    /** Like showSquareCrop, but crops to any box shape (boxW x boxH) so wide photos fill wide tiles. */
    public static void showCover(ImageView view, Image img, double boxW, double boxH) {
        view.setImage(img);
        if (boxW <= 0 || boxH <= 0) return;
        Runnable crop = () -> {
            double w = img.getWidth();
            double h = img.getHeight();
            if (w <= 0 || h <= 0) return;
            double target = boxW / boxH;
            double cw;
            double ch;
            if (w / h > target) { ch = h; cw = h * target; }   // photo is wider than the box: trim the sides
            else { cw = w; ch = w / target; }                  // photo is taller than the box: trim top/bottom
            view.setViewport(new Rectangle2D((w - cw) / 2, (h - ch) / 2, cw, ch));
        };
        if (img.getProgress() >= 1.0) {
            crop.run();
        } else {
            img.progressProperty().addListener((obs, was, now) -> {
                if (now.doubleValue() >= 1.0) crop.run();
            });
        }
    }
}
