package com.template.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

/** A loaded FXML screen: the node to put on screen plus its controller. */
public final class FxView<C> {

    private final Node root;
    private final C controller;

    private FxView(Node root, C controller) {
        this.root = root;
        this.controller = controller;
    }

    public Node getRoot() { return root; }

    public C getController() { return controller; }

    /** @param resource path from the resources root, e.g. "/com/template/browse/listing_card.fxml" */
    public static <C> FxView<C> load(String resource) {
        URL url = FxView.class.getResource(resource);
        if (url == null) throw new IllegalStateException("FXML not found on classpath: " + resource);
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Node root = loader.load();
            return new FxView<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + resource, e);
        }
    }
}