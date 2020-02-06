package com.axelor.apps.gst.tax;

import com.axelor.apps.account.db.FiscalPosition;
import com.axelor.apps.account.db.Tax;
import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.account.db.repo.TaxRepository;
import com.axelor.apps.account.service.AccountManagementServiceAccountImpl;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.exceptions.IExceptionMessage;
import com.axelor.apps.base.service.app.AppService;
import com.axelor.apps.base.service.tax.FiscalPositionService;
import com.axelor.apps.base.service.tax.TaxService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GstTaxLine extends AccountManagementServiceAccountImpl {

  @Inject AppService appService;

  @Inject
  public GstTaxLine(FiscalPositionService fiscalPositionService, TaxService taxService) {
    super(fiscalPositionService, taxService);
  }

  @Override
  public TaxLine getTaxLine(
      LocalDate date,
      Product product,
      Company company,
      FiscalPosition fiscalPosition,
      boolean isPurchase)
      throws AxelorException {
    TaxLine taxLine = null;
    if (appService.isApp("gst")) {
      Tax tax = this.getProductTax(product, company, fiscalPosition, isPurchase);
      if (tax != null) {
        if (tax.getActiveTaxLine() != null && product.getGstRate().compareTo(BigDecimal.ZERO) != 0) {
          if (tax.getActiveTaxLine().getValue().compareTo(product.getGstRate().divide(new BigDecimal(100))) == 0) 
        	  taxLine = tax.getActiveTaxLine();
        } else if (tax.getActiveTaxLine() != null) {
          taxLine = tax.getActiveTaxLine();
        }
      }
      if (taxLine == null) {
        List<Tax> taxList = Beans.get(TaxRepository.class).all().fetch();
        for (Tax t : taxList) {
          if (t.getCode().contains("GST") && t.getActiveTaxLine() != null && product.getGstRate() != null) {
            if (t.getActiveTaxLine().getValue().compareTo(product.getGstRate().divide(new BigDecimal(100))) == 0) 
            	taxLine = t.getActiveTaxLine();
          }
        }
      }
    }
    if (taxLine != null) 
    	return taxLine;
    throw new AxelorException(
        TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
        I18n.get(IExceptionMessage.ACCOUNT_MANAGEMENT_2),
        product.getCode());
  }
}
