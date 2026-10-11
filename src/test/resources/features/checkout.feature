@checkout
Feature: Checkout price integrity (Booking.com Demand API)
  As a traveller
  I want the price I am shown before checkout to be the price I pay
  So that I can trust booking.com with my reservation

  @demand-partner
  Scenario: The price quoted at availability is the price previewed at checkout
    Given a stay of 3 nights for 2 adults starting 45 days from today
    When I search accommodations in "Cairo"
    And I check availability of the first accommodation found
    And I preview an order for its cheapest product
    Then the order preview total equals the availability price
    And the order preview is in the booker currency

  @demand-auth
  Scenario: Checkout is refused without partner credentials
    When an anonymous client previews an order
    Then the request is rejected as unauthenticated
