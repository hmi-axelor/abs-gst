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
package com.axelor.apps.bankpayment.service.bankorder;

import com.axelor.apps.account.db.InvoicePayment;
import com.axelor.apps.bankpayment.db.BankOrder;
import com.axelor.exception.AxelorException;
import com.google.inject.persist.Transactional;
import java.util.Collection;

public interface BankOrderMergeService {

  @Transactional(rollbackOn = {Exception.class})
  public BankOrder mergeBankOrders(Collection<BankOrder> bankOrders) throws AxelorException;

  /**
   * Merge bank orders from invoice payments.
   *
   * @param invoicePayments
   * @return
   * @throws AxelorException
   */
  BankOrder mergeFromInvoicePayments(Collection<InvoicePayment> invoicePayments)
      throws AxelorException;
}
