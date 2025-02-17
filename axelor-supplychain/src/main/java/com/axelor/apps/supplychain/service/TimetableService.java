/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.supplychain.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.supplychain.db.Timetable;
import com.axelor.exception.AxelorException;
import com.google.inject.persist.Transactional;
import java.util.List;

public interface TimetableService {

  @Transactional(rollbackOn = {Exception.class})
  public Invoice generateInvoice(Timetable timetable) throws AxelorException;

  public Invoice createInvoice(Timetable timetable) throws AxelorException;

  public List<InvoiceLine> createInvoiceLine(Invoice invoice, Timetable timetable)
      throws AxelorException;

  public SaleOrderLine findFirstSaleOrderLine(Timetable timetable);

  public PurchaseOrderLine findFirstPurchaseOrderLine(Timetable timetable);

  public void computeProductInformation(Timetable timetable) throws AxelorException;
}
