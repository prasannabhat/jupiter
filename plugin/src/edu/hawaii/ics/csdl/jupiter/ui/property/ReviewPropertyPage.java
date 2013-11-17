package edu.hawaii.ics.csdl.jupiter.ui.property;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

import edu.hawaii.ics.csdl.jupiter.ReviewException;
import edu.hawaii.ics.csdl.jupiter.ReviewI18n;
import edu.hawaii.ics.csdl.jupiter.file.FileResource;
import edu.hawaii.ics.csdl.jupiter.file.PropertyConstraints;
import edu.hawaii.ics.csdl.jupiter.file.PropertyResource;
import edu.hawaii.ics.csdl.jupiter.file.ReviewResource;
import edu.hawaii.ics.csdl.jupiter.file.property.Review;
import edu.hawaii.ics.csdl.jupiter.model.review.ReviewId;
import edu.hawaii.ics.csdl.jupiter.util.JupiterLogger;
import edu.hawaii.ics.csdl.jupiter.util.ReviewDialog;

/**
 * Provides configuration property page.
 * 
 * @author Takuya Yamashita
 * @version $Id: ReviewPropertyPage.java 84 2008-03-07 10:11:27Z jsakuda $
 */
public class ReviewPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

  public ReviewPropertyPage() {}

  /** Jupiter logger */
  private final JupiterLogger log = JupiterLogger.getLogger();

  private static final String TABLE_COLUMN = "TableColumn";
  private static final String COLUMN_KEY = "ColumnKey";
  private IProject project;
  private TableViewer tableViewer;
  private Button newButton;
  private Table table;
  private Button removeButton;
  private Button editButton;
  private Button exportButton;
  private Button importButton;
  private Composite composite;
  /** The column review ID key. */
  public static final String COLUMN_REVIEW_ID_KEY = "ReviewPropertyPage.label.column.reviewId";
  /** The column description key. */
  public static final String COLUMN_DESCRIPTION_KEY = "ReviewPropertyPage.label.column.description";
  /** The column date key. */
  public static final String COLUMN_DATE_KEY = "ReviewPropertyPage.label.column.date";

  /**
   * Creates content.
   * 
   * @param ancestor the composite.
   * @return the control.
   */
  @Override
  protected Control createContents(final Composite ancestor) {
    this.composite = ancestor;
    this.project = (IProject) getElement();
    noDefaultAndApplyButton();
    Composite parent = createsGeneralComposite(ancestor);
    createReviewIdTableContent(parent);
    createButtonsContent(parent);
    return parent;
  }

  /**
   * Creates view preference frame and return the child composite.
   * 
   * @param parent the parent composite.
   * @return the child composite.
   */
  private Composite createsGeneralComposite(final Composite parent) {
    Composite child = new Composite(parent, SWT.LEFT);
    FormLayout layout = new FormLayout();
    layout.marginWidth = 7;
    layout.marginHeight = 7;
    child.setLayout(layout);

    return child;
  }

  /**
   * Creates review id table.
   * 
   * @param parent the composite.
   */
  private void createReviewIdTableContent(final Composite parent) {
    this.table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
    FormData tableData = new FormData();
    tableData.left = new FormAttachment(0, 0);
    tableData.right = new FormAttachment(80, 0);
    tableData.top = new FormAttachment(0, 0);
    tableData.bottom = new FormAttachment(100, 0);
    this.table.setLayoutData(tableData);
    this.table.setHeaderVisible(true);
    this.table.setLinesVisible(true);
    this.table.addListener(SWT.Selection, new Listener() {

      public void handleEvent(final Event e) {
        handleReviewIdSelection();
      }
    });
    TableColumn columnReviewId = new TableColumn(this.table, SWT.NONE);
    columnReviewId.setText(ReviewI18n.getString(COLUMN_REVIEW_ID_KEY));
    columnReviewId.setData(COLUMN_KEY, COLUMN_REVIEW_ID_KEY);
    TableColumn columnDescription = new TableColumn(this.table, SWT.NONE);
    String description = ReviewI18n.getString(COLUMN_DESCRIPTION_KEY);
    columnDescription.setText(description);
    columnDescription.setData(COLUMN_KEY, COLUMN_DESCRIPTION_KEY);
    TableColumn columnDate = new TableColumn(this.table, SWT.NONE);
    columnDate.setText(ReviewI18n.getString(COLUMN_DATE_KEY));
    columnDate.setData(COLUMN_KEY, COLUMN_DATE_KEY);

    List<TableColumn> columnList = new ArrayList<TableColumn>();
    columnList.add(columnReviewId);
    columnList.add(columnDescription);
    columnList.add(columnDate);
    hookSelectionListener(columnList);

    TableLayout tableLayout = new TableLayout();
    tableLayout.addColumnData(new ColumnWeightData(22));
    tableLayout.addColumnData(new ColumnWeightData(48));
    tableLayout.addColumnData(new ColumnWeightData(20));
    this.table.setLayout(tableLayout);

    this.tableViewer = new TableViewer(this.table);
    this.tableViewer.setLabelProvider(new ReviewPropertyLabelProvider());
    this.tableViewer.setContentProvider(new ReviewPropertyContentProvider());
    this.tableViewer.setSorter(ReviewPropertyViewerSorter.getViewerSorter(COLUMN_DATE_KEY));
    this.tableViewer.setInput(PropertyResource.getInstance(this.project, true).getReviewIdList());
    // this.tableViewer.
    // this.table.

    // routine for the version 1 compatibility.
    // if (tableViewer.getTable().getItemCount() <= 0) {
    // // version 1.
    // List<ReviewId> reviewIdList = Ver1PropertyHelper.getReviewIdList(this.project.getName(),
    // false);
    // IFile jupiterConfigFile = project.getFile(PropertyXmlSerializer.PROPERTY_XML_FILE);
    // FileResource.remove(new IFile[] { jupiterConfigFile });
    // // create version 2 .jupiter file.
    // String defaultName = PropertyConstraints.DEFAULT_REVIEW_ID;
    // PropertyResource propertyResource = null;
    // for (Iterator<ReviewId> i = reviewIdList.iterator(); i.hasNext();) {
    // ReviewId reviewId = (ReviewId) i.next();
    // propertyResource = PropertyResource.getInstance(this.project, true);
    // ReviewResource reviewResource = propertyResource.getReviewResource(defaultName, true);
    // ReviewId defaultReviewId = reviewResource.getReviewId();
    // String directory = defaultReviewId.getDirectory();
    // reviewId.setDirectory(directory);
    // reviewResource.setReviewId(reviewId);
    // try {
    // propertyResource.addReviewResource(reviewResource);
    // }
    // catch (ReviewException e) {
    // log.debug(e.getMessage());
    // }
    // }
    // tableViewer.setInput(propertyResource.getReviewIdList());
    // }
    this.tableViewer.addDoubleClickListener(new IDoubleClickListener() {

      public void doubleClick(final DoubleClickEvent event) {
        editReviewId();
      }
    });
  }

  /**
   * Creates buttons content.
   * 
   * @param parent the parent.
   */
  private void createButtonsContent(final Composite parent) {
    this.newButton = new Button(parent, SWT.PUSH);
    this.newButton.setText(ReviewI18n.getString("ReviewPropertyPage.label.button.new"));
    this.newButton.addListener(SWT.Selection, new Listener() {

      public void handleEvent(final Event e) {
        addReviewId();
      }
    });
    FormData newButtonData = new FormData();
    newButtonData.top = new FormAttachment(this.table, 0, SWT.TOP);
    newButtonData.left = new FormAttachment(this.table, 10);
    newButtonData.right = new FormAttachment(100, 0);
    this.newButton.setLayoutData(newButtonData);

    this.editButton = new Button(parent, SWT.PUSH);
    this.editButton.setText(ReviewI18n.getString("ReviewPropertyPage.label.button.edit"));
    this.editButton.setEnabled(false);
    this.editButton.addListener(SWT.Selection, new Listener() {

      public void handleEvent(final Event e) {
        editReviewId();
      }
    });
    FormData editButtonData = new FormData();
    editButtonData.top = new FormAttachment(this.newButton, 5);
    editButtonData.left = new FormAttachment(this.newButton, 0, SWT.LEFT);
    editButtonData.right = new FormAttachment(100, 0);
    this.editButton.setLayoutData(editButtonData);

    this.removeButton = new Button(parent, SWT.PUSH);
    this.removeButton.setText(ReviewI18n.getString("ReviewPropertyPage.label.button.remove"));
    this.removeButton.setEnabled(false);
    this.removeButton.addListener(SWT.Selection, new Listener() {

      public void handleEvent(final Event e) {
        removeReviewId();
      }
    });
    FormData removeButtonData = new FormData();
    removeButtonData.top = new FormAttachment(this.editButton, 5);
    removeButtonData.left = new FormAttachment(this.newButton, 0, SWT.LEFT);
    removeButtonData.right = new FormAttachment(100, 0);
    this.removeButton.setLayoutData(removeButtonData);

    this.exportButton = new Button(parent, SWT.NONE);
    this.exportButton.addListener(SWT.Selection, new Listener() {

      @Override
      public void handleEvent(final Event event) {
        exportReviewId();

      }
    });
    this.exportButton.setText(ReviewI18n.getString("ReviewPropertyPage.label.button.export"));
    this.exportButton.setEnabled(false);

    FormData exportButtonData = new FormData();
    exportButtonData.top = new FormAttachment(this.removeButton, 5);
    exportButtonData.left = new FormAttachment(this.newButton, 0, SWT.LEFT);
    exportButtonData.right = new FormAttachment(100, 0);

    this.exportButton.setLayoutData(exportButtonData);

    this.importButton = new Button(parent, SWT.NONE);
    this.importButton.addListener(SWT.Selection, new Listener() {

      @Override
      public void handleEvent(final Event event) {
        importReviewId();

      }
    });
    this.importButton.setText(ReviewI18n.getString("ReviewPropertyPage.label.button.import"));
    this.importButton.setEnabled(true);

    FormData importButtonData = new FormData();
    importButtonData.top = new FormAttachment(this.exportButton, 5);
    importButtonData.left = new FormAttachment(this.newButton, 0, SWT.LEFT);
    importButtonData.right = new FormAttachment(100, 0);

    this.importButton.setLayoutData(importButtonData);
  }

  /**
   * Hooks selection listener for each <code>TableColumn</code> element of the list.
   * 
   * @param columnList the list of the <code>TableColumn</code> elements.
   */
  private void hookSelectionListener(final List<TableColumn> columnList) {
    for (TableColumn column : columnList) {
      column.addListener(SWT.Selection, new Listener() {

        public void handleEvent(final Event event) {
          String columnKey = (String) event.widget.getData(COLUMN_KEY);
          sortBy(columnKey);
        }
      });
    }
  }

  /**
   * Sorts by the <code>String</code> columnKey.
   * 
   * @param columnKey the <code>String</code> columnKey.
   */
  protected void sortBy(final String columnKey) {
    ViewerSorter viewerSorter = ReviewPropertyViewerSorter.getViewerSorter(columnKey);
    if (viewerSorter != null) {
      ViewerSorter previousSorter = this.tableViewer.getSorter();
      if (previousSorter == viewerSorter) {
        ReviewPropertyViewerSorter.setReverse(!ReviewPropertyViewerSorter.isReverse());
        // Resets sorter.
        this.tableViewer.setSorter(null);
      }
      else {
        ReviewPropertyViewerSorter.setReverse(false);
      }
      this.tableViewer.setSorter(viewerSorter);
    }
  }

  /**
   * Adds review ID so as to open review id addition wizard.
   */
  private void addReviewId() {
    ReviewDialog.processConfigWizardDialog(this.project);
    List<ReviewId> reviewIdList = PropertyResource.getInstance(this.project, true).getReviewIdList();
    this.tableViewer.setInput(reviewIdList);
  }

  /**
   * Edits the selected review ID.
   */
  private void editReviewId() {
    IStructuredSelection selection = (IStructuredSelection) this.tableViewer.getSelection();
    if (selection.size() != 1) {
      MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
          ReviewI18n.getString("ReviewIdEditDialog.selectionerror.title"),
          ReviewI18n.getString("ReviewIdEditDialog.selectionerror.message"));
      return;
    }
    ReviewId reviewId = (ReviewId) selection.getFirstElement();
    Dialog dialog = new ReviewIdEditDialog(this.composite.getShell(), this.project, reviewId);
    dialog.open();
    this.tableViewer.setInput(PropertyResource.getInstance(this.project, true).getReviewIdList());

  }

  /**
   * Removes the selected review ID
   */
  private void removeReviewId() {
    IStructuredSelection selection = (IStructuredSelection) this.tableViewer.getSelection();
    Iterator<ReviewId> iterator = selection.iterator();

    while (iterator.hasNext()) {
      ReviewId reviewId = iterator.next();
      IFile[] reviewIFiles = FileResource.getReviewIFiles(this.project, reviewId);
      Dialog dialog = new ReviewIdRemovalDialog(this.composite.getShell(), reviewIFiles);
      dialog.open();
      if (dialog.getReturnCode() == Window.OK) {
        // remove review files associated with the review id.
        FileResource.remove(reviewIFiles);
        try {
          PropertyResource propertyResource = PropertyResource.getInstance(this.project, false);
          propertyResource.removeReviewResource(reviewId);
        }
        catch (ReviewException e) {
          this.log.error(e);
        }
      }
    }
    this.tableViewer.setInput(PropertyResource.getInstance(this.project, true).getReviewIdList());
  }

  /**
   * Exports the selected Review Ids to external file While exporting, it strips off the instance specific data like
   * date, reviewers etc.
   */
  private void exportReviewId() {
    IStructuredSelection selection = (IStructuredSelection) this.tableViewer.getSelection();
    Iterator<ReviewId> iterator = selection.iterator();
    String fileName = "D:\\temp\\jupiter.xml";
    File f = new File(fileName);
    try {
      boolean status = PropertyResource.getInstance(this.project, true).exportReviews(iterator, f);
      MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
          ReviewI18n.getString("ReviewIdEditDialog.export.suscess.title"),
          ReviewI18n.getString("ReviewIdEditDialog.export.suscess.message"));
    }
    catch (ReviewException e) {
      MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
          ReviewI18n.getString("ReviewIdEditDialog.export.failure.title"),
          ReviewI18n.getString("ReviewIdEditDialog.export.failure.message"));
      this.log.error(e);
    }
  }

  /**
   * Import the reviews from the given review template.. Throw error if the review already exists
   */
  private void importReviewId() {
    String fileName = "D:\\temp\\jupiter.xml";
    File f = new File(fileName);
    PropertyResource propertyResource = PropertyResource.getInstance(this.project, true);
    try {
      // List of existing review names
      List<String> reviewNames = new ArrayList<String>();
      for (ReviewId reviewId : propertyResource.getReviewIdList()) {
        reviewNames.add(reviewId.getReviewId());
      }

      // Get reviews from the given file
      List<Review> reviews = propertyResource.getReviews(f);

      // Check if any review already exists
      for (Review review : reviews) {
        if (reviewNames.contains(review.getId())) {
          MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
              ReviewI18n.getString("ReviewIdEditDialog.error"),
              review.getId() + " : " + ReviewI18n.getString("ReviewIdEditDialog.import.duplicate"));
          // Exit if the review already exists
          return;
        }
      }

      // Add the imported reviews to project
      for (Review review : reviews) {
        propertyResource.addReviewResource(new ReviewResource(review));
      }
      this.tableViewer.setInput(propertyResource.getReviewIdList());
    }
    catch (ReviewException e) {
      MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
          ReviewI18n.getString("ReviewIdEditDialog.error"),
          ReviewI18n.getString("ReviewIdEditDialog.import.failure.message"));
      this.log.error(e);
    }
  }

  /**
   * Handles review id selection
   */
  protected void handleReviewIdSelection() {
    IStructuredSelection selection = (IStructuredSelection) this.tableViewer.getSelection();
    boolean isSelected = false;
    if (selection.size() >= 1) {
      isSelected = true;
    }
    this.newButton.setEnabled(isSelected);
    this.editButton.setEnabled(isSelected);
    this.removeButton.setEnabled(isSelected);
    this.exportButton.setEnabled(isSelected);

    Iterator<ReviewId> it = selection.iterator();
    // If Default review Id is selected, then disable remove button
    while (it.hasNext()) {
      ReviewId reviewId = it.next();
      if (reviewId.getReviewId().equals(PropertyConstraints.DEFAULT_REVIEW_ID)) {
        this.removeButton.setEnabled(false);
        break;
      }
    }

  }
}
