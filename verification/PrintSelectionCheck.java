import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.io.HomeFileRecorder;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.HomePrint;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.FurnitureTable;
import com.eteks.sweethome3d.swing.HomePDFPrinter;
import com.eteks.sweethome3d.swing.IconManager;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.SwingUtilities;

/** Regression check for PDF export with a selected furniture row in the sample home. */
public class PrintSelectionCheck {
  public static void main(String[] args) throws Exception {
    System.setProperty("com.eteks.sweethome3d.no3D", "true");
    Home home = new HomeFileRecorder().readHome("samples/MultiLevelHome.sh3d");
    try {
      SwingUtilities.invokeAndWait(() -> verifySample(home));
    } finally {
      IconManager.getInstance().clear();
    }
    System.out.println("PDF export preserved the selected furniture and view with and without a table filter.");
  }

  private static void verifySample(Home home) {
    UserPreferences preferences = new DefaultUserPreferences();
    HomeController controller = new HomeController(home, preferences, new SwingViewFactory(),
        new FileContentManager(preferences));
    controller.getHomeController3D(); // Install the observer camera's level-change listener.
    FurnitureTable table = (FurnitureTable)controller.getFurnitureController().getView();
    table.setSize(table.getPreferredSize());
    table.doLayout();
    table.getTableHeader().setSize(table.getWidth(), table.getTableHeader().getPreferredSize().height);

    Level level = home.getLevels().get(2);
    home.setSelectedLevel(level);
    HomePieceOfFurniture selected = home.getFurniture().stream()
        .filter(piece -> piece.getLevel() == level).findFirst().get();
    home.setPrint(new HomePrint(HomePrint.PaperOrientation.PORTRAIT, 612, 792, 36, 36, 36, 36,
        true, true, false, null, null, null));
    HomePDFPrinter printer = new HomePDFPrinter(home, preferences, controller, table.getFont());
    verifyExport(home, table, printer, selected);
    table.setFurnitureFilter((model, piece) -> piece.getLevel() == level);
    verifyExport(home, table, printer, selected);
  }

  private static void verifyExport(Home home, FurnitureTable table, HomePDFPrinter printer,
                                   HomePieceOfFurniture selected) {
    home.setSelectedItems(Collections.singletonList(selected));
    home.getObserverCamera().setZ(100f);
    int[] selectedRows = table.getSelectedRows();
    int rowCount = table.getRowCount();
    FurnitureTable.FurnitureFilter filter = table.getFurnitureFilter();
    if (selectedRows.length == 0) {
      throw new AssertionError("Expected a highlighted furniture row before export");
    }
    try {
      printer.write(new ByteArrayOutputStream());
    } catch (IOException ex) {
      throw new AssertionError("Could not export the sample home", ex);
    }
    if (!home.getSelectedItems().equals(Collections.singletonList(selected))
        || !Arrays.equals(selectedRows, table.getSelectedRows())) {
      throw new AssertionError("PDF export changed the selected furniture or its highlighted rows");
    }
    if (home.getSelectedLevel() != selected.getLevel() || home.getObserverCamera().getZ() != 100f
        || table.getFurnitureFilter() != filter || table.getRowCount() != rowCount) {
      throw new AssertionError("PDF export changed the active level, observer height, or filtered table");
    }
  }
}
