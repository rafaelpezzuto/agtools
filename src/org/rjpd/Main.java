package org.rjpd;

import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.plugin.attribute.AttributeEqualBuilder;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;
import org.wouterspekkink.plugins.layout.eventgraph.TimeForce;
import org.wouterspekkink.plugins.layout.eventgraph.TimeForceBuilder;
import org.wouterspekkink.plugins.metric.lineage.Lineage;

import java.awt.*;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class Main {

    private ProjectController projectController;
    private Workspace workspace;
    private static GraphModel graphModel;
    private String slug;

    private void openProject(File inGephi) {
        projectController = Lookup.getDefault().lookup(ProjectController.class);
        projectController.openProject(inGephi).run();
    }

    private void configureProject() {
        workspace = projectController.getCurrentWorkspace();
        graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
    }

    private void obtainLineage(String nodeId) {
        Lineage lineage = new Lineage();
        lineage.setOrigin(nodeId);
        lineage.execute(graphModel);

        FilterController fc = Lookup.getDefault().lookup(FilterController.class);
        AttributeEqualBuilder.EqualStringFilter equalStringFilter = new AttributeEqualBuilder.EqualStringFilter.Node(graphModel.getNodeTable().getColumn("Lineage"));
        equalStringFilter.setUseRegex(true);
        equalStringFilter.setPattern("Descendant|Origin");
        equalStringFilter.init(graphModel.getGraph());
        Query query = fc.createQuery(equalStringFilter);
        GraphView view = fc.filter(query);
        graphModel.setVisibleView(view);
        fc.exportToNewWorkspace(query);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.projectController.deleteWorkspace(this.workspace);
        this.workspace = this.projectController.getCurrentWorkspace();
    }

    private void addColumnOrder() {
        graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        graphModel.getNodeTable().addColumn("Order", Integer.class);
        int minAnoTitulacao = 9999;

        for (Node n : graphModel.getGraph().getNodes()) {
            int currentAnoTitulacao = (int) n.getAttribute("ano_titulacao integer");
            if (currentAnoTitulacao < minAnoTitulacao) {
                minAnoTitulacao = currentAnoTitulacao;
            }
        }

        for (Node n : graphModel.getGraph().getNodes()) {
            n.setAttribute("Order", (int) n.getAttribute("ano_titulacao integer") - minAnoTitulacao);
        }
    }

    private void setNodeColor(){
        Color red = new Color(245, 0, 15);
        Color green = new Color(7, 190, 56);
        Color blue = new Color(16, 186, 237);
        for (Node n : graphModel.getGraph().getNodes()){
            int distanceDescendant = (int) n.getAttribute("DistanceDescendant");
            if (distanceDescendant == 0) {
                n.setColor(red);
            } else if (distanceDescendant == 1){
                n.setColor(green);
            } else {
                n.setColor(blue);
            }
        }
    }

    private void applyEventGraphLayout(){
        TimeForceBuilder tfb = new TimeForceBuilder();
        TimeForce tf = new TimeForce(tfb);

        tf.setGraphModel(graphModel);
        tf.setOrder(graphModel.getNodeTable().getColumn("Order"));

        tf.setVertical(true);
        tf.setStrongGravityMode(true);

        tf.setScalingRatio(200.0);

        int maxOrder = 0;
        for (Node n : graphModel.getGraph().getNodes()) {
            int currentOrder = (int) n.getAttribute("Order");
            if (currentOrder > maxOrder) {
                maxOrder = currentOrder;
            }
        }
        tf.setOrderScale((double) maxOrder);

        tf.getBuilder().buildLayout();
        tf.initAlgo();
        for (int i = 0; i < 100 && tf.canAlgo(); i++) {
            tf.goAlgo();
        }
        tf.endAlgo();

    }

    private void renameNewWorkspace() {
        for (Node n : graphModel.getGraph().getNodes()) {
            if (n.getAttribute("Lineage").equals("Origin")) {
                slug = n.getAttribute("slug varchar").toString();
                break;
            }

        }
        this.projectController.renameWorkspace(this.workspace, slug);
    }

    private void saveProject(File outFile) {
        projectController.saveProject(projectController.getCurrentProject(), outFile).run();
    }

    public static void main(String[] args) {
        String inGephi = args[0];
        String nodeId = args[1];

        Main m = new Main();

        System.out.println("Opening project " + inGephi + "...");
        m.openProject(new File(inGephi));

        System.out.println("Configuring project...");
        m.configureProject();

        System.out.println("Obtaining lineage for node " + nodeId + "...");
        m.obtainLineage(nodeId);

        System.out.println("Adding column order...");
        m.addColumnOrder();

        System.out.println("Coloring nodes...");
        m.setNodeColor();

        System.out.println("Applying event graph layout...");
        m.applyEventGraphLayout();

        System.out.println("Saving lineage...");
        m.renameNewWorkspace();
        m.saveProject(new File("linhagem-" + m.slug + ".gephi"));
    }
}
