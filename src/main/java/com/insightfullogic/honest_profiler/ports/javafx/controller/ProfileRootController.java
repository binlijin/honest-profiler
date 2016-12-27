/**
 * Copyright (c) 2014 Richard Warburton (richard.warburton@gmail.com)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 **/
package com.insightfullogic.honest_profiler.ports.javafx.controller;

import static com.insightfullogic.honest_profiler.core.Monitor.pipe;
import static com.insightfullogic.honest_profiler.core.Monitor.pipeFile;
import static com.insightfullogic.honest_profiler.ports.javafx.ViewType.FLAME;
import static com.insightfullogic.honest_profiler.ports.javafx.ViewType.FLAT;
import static com.insightfullogic.honest_profiler.ports.javafx.util.ConversionUtil.getStringConverterForType;

import java.io.File;

import com.insightfullogic.honest_profiler.core.collector.FlameGraphCollector;
import com.insightfullogic.honest_profiler.core.collector.LogCollector;
import com.insightfullogic.honest_profiler.core.parser.LogEventListener;
import com.insightfullogic.honest_profiler.core.parser.LogEventPublisher;
import com.insightfullogic.honest_profiler.core.profiles.Profile;
import com.insightfullogic.honest_profiler.core.profiles.ProfileListener;
import com.insightfullogic.honest_profiler.core.sources.CantReadFromSourceException;
import com.insightfullogic.honest_profiler.core.sources.VirtualMachine;
import com.insightfullogic.honest_profiler.ports.javafx.ViewType;
import com.insightfullogic.honest_profiler.ports.javafx.model.ProfileContext;
import com.insightfullogic.honest_profiler.ports.sources.FileLogSource;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public class ProfileRootController implements ProfileListener, ProfileController
{
    @FXML
    private ChoiceBox<ViewType> viewChoice;
    @FXML
    private Label traceCount;
    @FXML
    private AnchorPane content;
    @FXML
    private FlatViewController flatController;
    @FXML
    private TreeViewController treeController;
    @FXML
    private FlameViewController flameController;

    private ProfileContext profileContext;

    @FXML
    public void initialize()
    {
        profileContext = new ProfileContext();
        profileContext.addListeners(this);

        flatController.setProfileContext(profileContext);
        treeController.setProfileContext(profileContext);

        viewChoice.getSelectionModel().selectedItemProperty()
            .addListener((property, oldValue, newValue) -> show(newValue));
        viewChoice.setConverter(getStringConverterForType(ViewType.class));
        viewChoice.getItems().addAll(ViewType.values());
        viewChoice.getSelectionModel().select(FLAT);
    }

    // Instance Accessors

    @Override
    public ProfileContext getProfileContext()
    {
        return profileContext;
    }

    // ProfileListener Implementation

    /**
     * Not threadsafe: must be run on JavaFx thread.
     */
    @Override
    public void accept(Profile profile)
    {
        traceCount.setText(profile.getTraceCount() + " samples");
    }

    // Profiling startup

    public ProfileContext initializeProfile(Object source)
    {
        if (source instanceof VirtualMachine)
        {
            VirtualMachine vm = (VirtualMachine) source;
            profileContext.setName("Live - " + vm.getDisplayName() + " (" + vm.getId() + ")");
            monitor((VirtualMachine) source);
        }
        else
        {
            File file = (File) source;
            profileContext.setName("File - " + file.getName());
            openFile((File) source);
        }
        return profileContext;
    }

    private void openFile(File file)
    {
        final LogEventListener collector = new LogEventPublisher()
            .publishTo(new LogCollector(profileContext, false))
            .publishTo(new FlameGraphCollector(flameController));
        pipe(new FileLogSource(file), collector, false).run();
    }

    private void monitor(VirtualMachine machine)
    {
        try
        {
            pipeFile(machine.getLogSource(), profileContext);
        }
        catch (CantReadFromSourceException crfse)
        {
            throw new RuntimeException(crfse.getMessage(), crfse);
        }
    }

    // View Switch

    private void show(ViewType viewType)
    {
        for (int i = 0; i < ViewType.values().length; i++)
        {
            Node child = content.getChildren().get(i);
            child.setManaged(viewType.ordinal() == i);
            child.setVisible(viewType.ordinal() == i);
        }

        if (viewType == FLAME)
        {
            flameController.refreshFlameView();
        }
    }
}
