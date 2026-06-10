Feature: AI Chat Service
  As a user of the chat application
  I want to send messages and receive AI responses
  So that I can have intelligent conversations

  Background:
    Given the chat service is available

  Scenario: Send a message and receive response
    When the user sends message "Hello, how are you?"
    Then the system should return a non-empty response
    And the response should be a string

  Scenario: Health check endpoint
    When the user calls the health endpoint
    Then the response status should be "UP"

  Scenario: Empty message handling
    When the user sends an empty message
    Then the system should return an appropriate message
