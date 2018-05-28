/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.fulfillment.service.referencedata;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class FacilityReferenceDataServiceTest extends BaseReferenceDataServiceTest<FacilityDto> {
  
  FacilityReferenceDataService service;

  @Before
  public void before() {
    service = (FacilityReferenceDataService) prepareService();
  }
  
  @Override
  protected BaseReferenceDataService<FacilityDto> getService() {
    return new FacilityReferenceDataService();
  }

  @Override
  protected FacilityDto generateInstance() {
    return new FacilityDto();
  }
  
  @Test
  public void shouldFindFacilitiesByIds() {
    // given
    UUID id = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    List<UUID> ids = Arrays.asList(id, id2);

    FacilityDto facility = generateInstance();
    facility.setId(id);
    FacilityDto anotherFacility = generateInstance();
    anotherFacility.setId(id2);

    Map<String, Object> payload = new HashMap<>();
    payload.put("id", ids);
    ResponseEntity response = mock(ResponseEntity.class);

    // when
    when(response.getBody()).thenReturn(new FacilityDto[]{facility, anotherFacility});

    when(restTemplate.exchange(
        any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(service.getArrayResultClass())
    )).thenReturn(response);

    Collection<FacilityDto> facilities = service.findByIds(ids);

    // then
    verify(restTemplate).exchange(
        uriCaptor.capture(), eq(HttpMethod.GET),
        entityCaptor.capture(), eq(service.getArrayResultClass())
    );
    assertTrue(facilities.contains(facility));
    assertTrue(facilities.contains(anotherFacility));

    String actualUrl = uriCaptor.getValue().toString();
    assertTrue(actualUrl.startsWith(service.getServiceUrl() + service.getUrl()));
    assertTrue(actualUrl.contains(id.toString()));
    assertTrue(actualUrl.contains(id2.toString()));

    assertAuthHeader(entityCaptor.getValue());
  }
}
