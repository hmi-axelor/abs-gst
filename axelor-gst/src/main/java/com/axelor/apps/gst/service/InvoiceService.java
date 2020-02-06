package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Company;

public interface InvoiceService {

  public Invoice calculateInvoiceGst(Invoice invoice);

  public Boolean isStateDifferent(Company company, Address address);
}
