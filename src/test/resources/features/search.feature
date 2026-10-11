@search
Feature: Destination search
  As a traveller
  I want booking.com to understand the destination I type
  So that I land on relevant search results quickly

  @autocomplete
  Scenario Outline: The search box suggests the city I typed first
    When I type "<query>" in the destination search box
    Then the top suggestion is "<query>" in "<country>"
    And every suggestion has a destination id, type and label

    Examples:
      | query     | country     |
      | Cairo     | Egypt       |
      | Amsterdam | Netherlands |

  @web
  Scenario: Searching a destination serves its results page
    Given a stay of 3 nights for 2 adults starting 45 days from today
    When I search for "Cairo"
    Then the results page is served as HTML
    And the results page mentions "Cairo"
