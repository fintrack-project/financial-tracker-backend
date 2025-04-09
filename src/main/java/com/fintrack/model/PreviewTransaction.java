public class PreviewTransaction extends Transaction {
  private boolean markDelete;

  public boolean isMarkDelete() {
      return markDelete;
  }

  public void setMarkDelete(boolean markDelete) {
      this.markDelete = markDelete;
  }
}