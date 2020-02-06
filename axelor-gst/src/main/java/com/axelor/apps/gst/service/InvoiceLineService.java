package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.InvoiceLine;
import java.util.List;

public interface InvoiceLineService {

  public InvoiceLine calculateInvoiceLineGst(InvoiceLine invoiceLine, Boolean isStateDiff);

  public List<InvoiceLine> getInvoiceItemsById(List<Integer> products);
}
