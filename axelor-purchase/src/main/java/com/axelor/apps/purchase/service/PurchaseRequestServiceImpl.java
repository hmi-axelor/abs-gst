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
package com.axelor.apps.purchase.service;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.purchase.db.PurchaseRequest;
import com.axelor.apps.purchase.db.repo.PurchaseOrderLineRepository;
import com.axelor.apps.purchase.db.repo.PurchaseOrderRepository;
import com.axelor.apps.purchase.db.repo.PurchaseRequestRepository;
import com.axelor.auth.AuthUtils;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PurchaseRequestServiceImpl implements PurchaseRequestService {

  @Inject private PurchaseRequestRepository purchaseRequestRepo;
  @Inject private PurchaseOrderService purchaseOrderService;
  @Inject private PurchaseOrderLineService purchaseOrderLineService;
  @Inject private PurchaseOrderRepository purchaseOrderRepo;
  @Inject private PurchaseOrderLineRepository purchaseOrderLineRepo;

  @Transactional
  @Override
  public void confirmCart() {

    List<PurchaseRequest> purchaseRequests =
        purchaseRequestRepo
            .all()
            .filter("self.statusSelect = 1 and self.createdBy = ?1", AuthUtils.getUser())
            .fetch();

    for (PurchaseRequest purchaseRequest : purchaseRequests) {
      purchaseRequest.setStatusSelect(2);
      purchaseRequestRepo.save(purchaseRequest);
    }
  }

  @Transactional
  @Override
  public void acceptRequest(List<PurchaseRequest> purchaseRequests) {

    for (PurchaseRequest purchaseRequest : purchaseRequests) {
      purchaseRequest.setStatusSelect(3);
      purchaseRequestRepo.save(purchaseRequest);
    }
  }

  @Transactional(rollbackOn = {Exception.class})
  @Override
  public List<PurchaseOrder> generatePo(
      List<PurchaseRequest> purchaseRequests, Boolean groupBySupplier, Boolean groupByProduct)
      throws AxelorException {

    Map<String, PurchaseOrder> purchaseOrderMap = new HashMap<>();

    for (PurchaseRequest purchaseRequest : purchaseRequests) {
      Product product = purchaseRequest.getProduct();
      String key = groupBySupplier ? getPurchaseOrderGroupBySupplierKey(purchaseRequest) : null;
      PurchaseOrder purchaseOrder;
      if (key != null && purchaseOrderMap.containsKey(key)) {
        purchaseOrder = purchaseOrderMap.get(key);
      } else {
        purchaseOrder = createPurchaseOrder(purchaseRequest);
        key = key == null ? purchaseRequest.getId().toString() : key;
        purchaseOrderMap.put(key, purchaseOrder);
      }
      PurchaseOrderLine purchaseOrderLine =
          groupByProduct && purchaseOrder != null
              ? getPoLineByProduct(product, purchaseOrder)
              : null;

      if (purchaseOrder == null) {
        purchaseOrder = createPurchaseOrder(purchaseRequest);
      }
      if (purchaseOrderLine == null) {
        purchaseOrderLine =
            purchaseOrderLineService.createPurchaseOrderLine(
                purchaseOrder,
                product,
                purchaseRequest.getNewProduct()
                    ? purchaseRequest.getProductTitle()
                    : product.getName(),
                purchaseRequest.getNewProduct() ? null : product.getDescription(),
                purchaseRequest.getQuantity(),
                purchaseRequest.getUnit());
        purchaseOrder.addPurchaseOrderLineListItem(purchaseOrderLine);
      } else {
        purchaseOrderLine.setQty(purchaseOrderLine.getQty().add(purchaseRequest.getQuantity()));
      }
      purchaseOrderLineService.compute(purchaseOrderLine, purchaseOrder);
      purchaseOrderService.computePurchaseOrder(purchaseOrder);
      purchaseOrderLineRepo.save(purchaseOrderLine);
    }
    List<PurchaseOrder> purchaseOrders =
        purchaseOrderMap.values().stream().collect(Collectors.toList());
    return purchaseOrders;
  }

  private PurchaseOrderLine getPoLineByProduct(Product product, PurchaseOrder purchaseOrder) {

    PurchaseOrderLine purchaseOrderLine =
        purchaseOrder.getPurchaseOrderLineList() != null
                && !purchaseOrder.getPurchaseOrderLineList().isEmpty()
            ? purchaseOrder
                .getPurchaseOrderLineList()
                .stream()
                .filter(poLine -> poLine.getProduct().equals(product))
                .findFirst()
                .orElse(null)
            : null;
    return purchaseOrderLine;
  }

  protected PurchaseOrder createPurchaseOrder(PurchaseRequest purchaseRequest)
      throws AxelorException {
    return purchaseOrderRepo.save(
        purchaseOrderService.createPurchaseOrder(
            AuthUtils.getUser(),
            purchaseRequest.getCompany(),
            null,
            purchaseRequest.getSupplierUser().getCurrency(),
            null,
            null,
            null,
            LocalDate.now(),
            null,
            purchaseRequest.getSupplierUser(),
            null));
  }

  protected String getPurchaseOrderGroupBySupplierKey(PurchaseRequest purchaseRequest) {
    return purchaseRequest.getSupplierUser().getId().toString();
  }
}
