package pipe.views;

import pipe.gui.ApplicationSettings;
import pipe.gui.Constants;
import pipe.gui.PetriNetTab;
import pipe.gui.widgets.oldwidges.ArcWeightEditorPanel;
import pipe.gui.widgets.oldwidges.EscapableDialog;
import pipe.historyActions.AddArcPathPoint;
import pipe.historyActions.ArcWeight;
import pipe.historyActions.HistoryItem;
import pipe.models.Arc;
import pipe.models.interfaces.IObserver;
import pipe.utilities.Copier;
import pipe.views.viewComponents.ArcPath;
import pipe.views.viewComponents.ArcPathPoint;
import pipe.views.viewComponents.NameLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.LinkedList;

public abstract class ArcView extends PetriNetViewComponent implements Cloneable, IObserver,Serializable
{
    private static final long serialVersionUID = 1L;
    LinkedList<NameLabel> weightLabel = new LinkedList<NameLabel>();
    LinkedList<MarkingView> _weight = new LinkedList<MarkingView>();

    private ConnectableView _source = null;
    private ConnectableView _target = null;
    // private boolean deleted = false; // Used for cleanup purposes

    private Arc _model;

    final ArcPath myPath = new ArcPath(this);

    // true if arc is not hidden when a bidirectional arc is used
    boolean inView = true;

    // bounds of arc need to be grown in order to avoid clipping problems
    final int zoomGrow = 10;

    ArcView(double startPositionXInput, double startPositionYInput,
            double endPositionXInput, double endPositionYInput,
            ConnectableView sourceInput,
            ConnectableView targetInput, LinkedList<MarkingView> weightInput,
            String idInput, Arc model)
    {
        _model = model;
        myPath.addPoint((float) startPositionXInput,(float) startPositionYInput, ArcPathPoint.STRAIGHT);
        myPath.addPoint((float) endPositionXInput, (float) endPositionYInput, ArcPathPoint.STRAIGHT);
        myPath.createPath();
        updateBounds();
        _id = idInput;
        setSource(sourceInput);
        setTarget(targetInput);
        setWeight(Copier.mediumCopy(weightInput));
    }

    ArcView(ConnectableView newSource)
    {
        _source = newSource;
        myPath.addPoint();
        myPath.addPoint();
        myPath.createPath();
    }

    ArcView()
    {
        super();
    }

    void setSource(ConnectableView sourceInput)
    {
        _source = sourceInput;
    }

    public void setTarget(ConnectableView targetInput)
    {
        _target = targetInput;
    }

    public HistoryItem setWeight(LinkedList<MarkingView> weightInput)
    {
        LinkedList<MarkingView> oldWeight = Copier.mediumCopy(_weight);
        return new ArcWeight(this, oldWeight, _weight);
    }

    public void setWeightLabelPosition()
    {
        int originalX = (int) (myPath.midPoint.x);
        int originalY = (int) (myPath.midPoint.y) - 10;
        int x = originalX;
        int y = originalY;
        int yCount = 0;
        for(NameLabel label : weightLabel)
        {
            if(yCount >= 4)
            {
                y = originalY;
                x += 17;
                yCount = 0;
            }
            // we must trim here as the removeLabelsFromArc adds blank spaces
            if(!label.getText().trim().equals("") && Integer.valueOf(label.getText()) > 0)
            {
                label.setPosition(x + label.getWidth() / 2 - 4, y);
                y += 10;
                yCount++;
            }
        }
    }

    // Removes any current labels from the arc
    void removeLabelsFromArc()
    {
        int x = (int) (myPath.midPoint.x);
        int y = (int) (myPath.midPoint.y) - 10;
        for(NameLabel label : weightLabel)
        {
            if(!label.getText().trim().equals("") && Integer.valueOf(label.getText()) > 0)
            {
                // Put blank spaces over previous label
                //	NameLabel newLabel = new NameLabel(zoom);
                int currentLength = label.getText().length();
                label.setText("");
                for(int i = 0; i < currentLength; i++)
                {
                    label.setText(label.getText() + " ");
                }
                label.setPosition(x + label.getWidth() / 2 - 4, y);
                y += 10;
            }
            getParent().remove(label);
        }
        weightLabel.clear();
    }

    public String getId()
    {
        if(_id != null)
        {
            return _id;
        }
        else
        {
            if(_source != null && _target != null)
            {
                return _source.getId() + " to " + _target.getId();
            }
        }
        return "";
    }

    public String getName()
    {
        return getId();
    }

    public ConnectableView getSource()
    {
        return _source;
    }

    public ConnectableView getTarget()
    {
        return _target;
    }

    public ConnectableView getTheOtherEndFor(ConnectableView connectableView)
    {
        if(_source == connectableView)
            return _target;
        return _source;
    }

    public double getStartPositionX()
    {
        return myPath.getPoint(0).getX();
    }

    public double getStartPositionY()
    {
        return myPath.getPoint(0).getY();
    }

    public LinkedList<MarkingView> getWeight()
    {
        return _weight;
    }

    public int getSimpleWeight()
    {
        return 1;
    }

    public void updateArcPosition()
    {
        if(_source != null)
        {
            _source.updateEndPoint(this);
        }
        if(_target != null)
        {
            _target.updateEndPoint(this);
        }
        myPath.createPath();
    }

    public void setEndPoint(double x, double y, boolean type)
    {
        myPath.setPointLocation(myPath.getEndIndex(), x, y);
        myPath.setPointType(myPath.getEndIndex(), type);
        updateArcPosition();
    }

    public void setTargetLocation(double x, double y)
    {
        myPath.setPointLocation(myPath.getEndIndex(), x, y);
        myPath.createPath();
        updateBounds();
        repaint();
    }

    public void setSourceLocation(double x, double y)
    {
        myPath.setPointLocation(0, x, y);
        myPath.createPath();
        updateBounds();
        repaint();
    }

    /**
     * Updates the bounding box of the arc component based on the arcs bounds
     */
    void updateBounds()
    {
        _bounds = myPath.getBounds();
        _bounds.grow(getComponentDrawOffset() + zoomGrow, getComponentDrawOffset()
                + zoomGrow);
        setBounds(_bounds);
    }

    public ArcPath getArcPath()
    {
        return myPath;
    }

    public boolean contains(int x, int y)
    {
        Point2D.Double point = new Point2D.Double(x + myPath.getBounds().getX()
                                                          - getComponentDrawOffset() - zoomGrow, y
                + myPath.getBounds().getY() - getComponentDrawOffset() - zoomGrow);
        if(!ApplicationSettings.getApplicationView().getCurrentTab().isInAnimationMode())
        {
            if(myPath.proximityContains(point) || _selected)
            {
                // show also if Arc itself selected
                myPath.showPoints();
            }
            else
            {
                myPath.hidePoints();
            }
        }

        return myPath.contains(point);
    }

    public void addedToGui()
    {
        // called by PetriNetTab / State viewer when adding component.
        _deleted = false;
        _markedAsDeleted = false;

        if(getParent() instanceof PetriNetTab)
        {
            myPath.addPointsToGui((PetriNetTab) getParent());
        }
        else
        {
            myPath.addPointsToGui((JLayeredPane) getParent());
        }
        updateArcPosition();
        Container parent = getParent();
        if(parent != null)
        {
            int i = 0;
            for(NameLabel label : weightLabel)
            {
                if(label.getParent() == null)
                {
                    i++;
                    parent.add(label);
                }
            }
        }
    }

    public void delete()
    {
        if(!_deleted)
        {
            for(NameLabel label : weightLabel)
            {
                getParent().remove(label);
            }
            myPath.forceHidePoints();
            super.delete();
            _deleted = true;
        }
    }

    public void setPathToTransitionAngle(int angle)
    {
        myPath.set_transitionAngle(angle);
    }

    public HistoryItem split(Point2D.Float mouseposition)
    {
        ArcPathPoint newPoint = myPath.splitSegment(mouseposition);
        return new AddArcPathPoint(this, newPoint);
    }

    public abstract String getType();

    public boolean inView()
    {
        return inView;
    }

    public TransitionView getTransition()
    {
        if(getTarget() instanceof TransitionView)
        {
            return (TransitionView) getTarget();
        }
        else
        {
            return (TransitionView) getSource();
        }
    }

    public void removeFromView()
    {
        if(getParent() != null)
        {
            for(NameLabel label : weightLabel)
            {
                getParent().remove(label);
            }
        }
        myPath.forceHidePoints();
        removeFromContainer();
    }

    public void addToView(PetriNetTab view)
    {
        if(getParent() != null)
        {
            for(NameLabel label : weightLabel)
            {
                getParent().add(label);
            }
        }
        myPath.showPoints();
        view.add(this);
    }

    public boolean getsSelected(Rectangle selectionRectangle)
    {
        if(_selectable)
        {
            ArcPath arcPath = getArcPath();
            if(arcPath.proximityIntersects(selectionRectangle))
            {
                arcPath.showPoints();
            }
            else
            {
                arcPath.hidePoints();
            }
            if(arcPath.intersects(selectionRectangle))
            {
                select();
                return true;
            }
        }
        return false;
    }

    public int getLayerOffset()
    {
        return Constants.ARC_LAYER_OFFSET;
    }

    public void translate(int x, int y)
    {
        // We don't translate an arc, we translate each selected arc point
    }

    public void zoomUpdate(int percent)
    {
        _zoomPercentage = percent;
        this.updateArcPosition();
        for(NameLabel label : weightLabel)
        {
            label.zoomUpdate(percent);
            label.updateSize();
        }
    }

    public void setZoom(int percent)
    {
        _zoomPercentage = percent;
    }

    public void undelete(PetriNetView model, PetriNetTab view)
    {
        if(this.isDeleted())
        {
            model.addPetriNetObject(this);
            view.add(this);
            getSource().addOutbound(this);
            getTarget().addInbound(this);
        }
    }

    /**
     * Method to clone an Arc object
     */
    public PetriNetViewComponent clone()
    {
        return super.clone();
    }

    public int getWeightOfTokenClass(String id)
    {
        if(_weight != null)
        {
            for(MarkingView m : _weight)
            {
                if(m.getToken().getID().equals(id))
                {
                    return m.getCurrentMarking();
                }
            }
        }
        return 0;
    }

    public void showEditor()
    {
        // Build interface
        EscapableDialog guiDialog = new EscapableDialog(ApplicationSettings.getApplicationView(),
                                                        "PIPE2", true);

        ArcWeightEditorPanel arcWeightEditor = new ArcWeightEditorPanel(
                guiDialog.getRootPane(), this, ApplicationSettings.getApplicationView().getCurrentPetriNetView(), ApplicationSettings.getApplicationView().getCurrentTab());

        guiDialog.add(arcWeightEditor);

        guiDialog.getRootPane().setDefaultButton(null);

        guiDialog.setResizable(false);

        // Make window fit contents' preferred size
        guiDialog.pack();

        // Move window to the middle of the screen
        guiDialog.setLocationRelativeTo(null);

        guiDialog.setVisible(true);

        guiDialog.dispose();
    }

    // Accessor function to check whether or not the Arc is tagged
    public boolean isTagged()
    {
        return false;
    }
}
