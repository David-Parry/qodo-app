package com.davidparry.agent.core.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for TemplateProcessor using Azure DevOps (ADO) git.push webhook payload.
 * Tests the merging of instruction templates with ADO JSON data.
 */
class InstructionADODataTemplateTest {

    private TemplateProcessor templateProcessor;
    private ObjectMapper objectMapper;

    private static final String ADO_GIT_PUSH_JSON = """
            {
              "subscriptionId": "00000000-0000-0000-0000-000000000000",
              "notificationId": 1,
              "id": "03c164c2-8912-4d5e-8009-3707d5f83734",
              "eventType": "git.push",
              "publisherId": "tfs",
              "message": {
                "text": "David pushed updates to qodo-code:trunk.",
                "html": "David pushed updates to qodo-code:trunk.",
                "markdown": "David pushed updates to `qodo-code`:`trunk`."
              },
              "detailedMessage": {
                "text": "David pushed a commit to qodo-code:trunk.\\n - Fixed bug in web.config file 33b55f7c",
                "html": "David pushed a commit to <a href=\\"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code\\">qodo-code</a>:<a href=\\"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code#version=GBtrunk\\">trunk</a>.\\n<ul>\\n<li>Fixed bug in web.config file <a href=\\"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code/commit/33b55f7cb7e7e245323987634f960cf4a6e6bc74\\">33b55f7c</a>\\n</ul>",
                "markdown": "David pushed a commit to [qodo-code](https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code):[trunk](https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code#version=GBtrunk).\\n* Fixed bug in web.config file [33b55f7c](https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code/commit/33b55f7cb7e7e245323987634f960cf4a6e6bc74)"
              },
              "resource": {
                "commits": [
                  {
                    "commitId": "33b55f7cb7e7e245323987634f960cf4a6e6bc74",
                    "author": {
                      "name": "David",
                      "email": "david@example.com",
                      "date": "2015-02-25T19:01:00Z"
                    },
                    "committer": {
                      "name": "David",
                      "email": "david@example.com",
                      "date": "2015-02-25T19:01:00Z"
                    },
                    "comment": "Fixed bug in web.config file",
                    "url": "https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code/commit/33b55f7cb7e7e245323987634f960cf4a6e6bc74"
                  }
                ],
                "refUpdates": [
                  {
                    "name": "refs/heads/trunk",
                    "oldObjectId": "aad331d8d3b131fa9ae03cf5e53965b51942618a",
                    "newObjectId": "33b55f7cb7e7e245323987634f960cf4a6e6bc74"
                  }
                ],
                "repository": {
                  "id": "76b3481e-f72a-496a-b7d1-ba015f1628b8",
                  "name": "qodo-code",
                  "url": "https://davidp0219@dev.azure.com/davidp0219/qodo-code/_apis/git/repositories/76b3481e-f72a-496a-b7d1-ba015f1628b8",
                  "project": {
                    "id": "6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c",
                    "name": "qodo-code",
                    "url": "https://davidp0219@dev.azure.com/davidp0219/qodo-code/_apis/projects/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c",
                    "state": "wellFormed",
                    "visibility": "unchanged",
                    "lastUpdateTime": "0001-01-01T00:00:00"
                  },
                  "defaultBranch": "refs/heads/trunk",
                  "remoteUrl": "https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code"
                },
                "pushedBy": {
                  "displayName": "David",
                  "id": "00067FFED5C7AF52@Live.com",
                  "uniqueName": "david@example.com"
                },
                "pushId": 14,
                "date": "2014-05-02T19:17:13.3309587Z",
                "url": "https://davidp0219@dev.azure.com/davidp0219/qodo-code/_apis/git/repositories/76b3481e-f72a-496a-b7d1-ba015f1628b8/pushes/14"
              },
              "resourceVersion": "1.0",
              "resourceContainers": {
                "collection": {
                  "id": "c12d0eb8-e382-443b-9f9c-c52cba5014c2"
                },
                "account": {
                  "id": "f844ec47-a9db-4511-8281-8b63f4eaf94e"
                },
                "project": {
                  "id": "be9b3917-87e6-42a4-a549-2bc06a7a878f"
                }
              },
              "createdDate": "2026-01-23T19:33:29.5724811Z"
            }
            """;

    private static final String ADO_INSTRUCTION_TEMPLATE = """
      You are a documentation agent that analyzes code changes from Azure DevOps push events and updates project documentation accordingly. Your role is to understand the existing documentation structure, analyze code changes, and create comprehensive documentation updates including architectural diagrams when significant changes are detected.

      ## INPUT CONTEXT
      The following variables contain information about the push event that triggered this workflow:
      - Repository Remote URL: {/resource/repository/remoteUrl}
      - Repository ID: {/resource/repository/id}
      - Collection ID: {/resourceContainers/collection/id}
      - Account ID: {/resourceContainers/account/id}
      - Project ID: {/resourceContainers/project/id}
      - Project Name: {/resource/repository/project/name}
      - Default Branch: {/resource/repository/defaultBranch}
      - Pushed By: {/resource/pushedBy/displayName}
      - Event Type: {/eventType}
      - Commit Message: {/message/text}
      - Detailed Message: {/detailedMessage/text}

      ## WORKFLOW

      ### Phase 1: Repository Setup
      1. Clone the git repository using the remote URL: {/resource/repository/remoteUrl}
      2. Checkout the branch that was pushed to (extract from the push event details)
      3. Use `get_project_tree` to understand the complete project structure

      ### Phase 2: Existing Documentation Analysis (MANDATORY)
      **Before making any changes, you MUST thoroughly review the existing documentation:**
      
      1. **Locate the docs folder**: Search for documentation directories in common locations:
         - `docs/`
         - `documentation/`
         - `doc/`
         - `.github/`
         - `wiki/`
         - Root level markdown files (README.md, CONTRIBUTING.md, etc.)
      
      2. **Read and analyze ALL existing documentation files**:
         - Use `terminal_execute_command` to list all files: `find docs -type f -name "*.md" 2>/dev/null || find . -maxdepth 2 -name "*.md"`
         - Read each documentation file to understand:
           * Current documentation structure and organization
           * Writing style and formatting conventions used
           * Existing architectural diagrams (look for mermaid blocks)
           * Table of contents and navigation patterns
           * Cross-references between documents
      
      3. **Document your findings**:
         - Note the documentation hierarchy
         - Identify the main entry points (README, index files)
         - Understand how different topics are organized
         - Note any existing design/architecture documentation

      ### Phase 3: Code Change Analysis (MANDATORY)
      **Analyze the diff between the pushed branch and the default branch:**
      
      1. **Generate the diff**: Use `terminal_execute_command` to run:
         ```bash
         git diff {/resource/repository/defaultBranch}...HEAD --stat
         git diff {/resource/repository/defaultBranch}...HEAD
         ```
      
      2. **Categorize the changes**:
         - **New files added**: List all new files and their purposes
         - **Modified files**: Identify what was changed and why
         - **Deleted files**: Note any removed functionality
         - **Renamed/moved files**: Track file relocations
      
      3. **Assess change significance**:
         - **Minor changes**: Bug fixes, typo corrections, small refactors
         - **Moderate changes**: New features, API additions, configuration changes
         - **Significant/Architectural changes**: New modules, design pattern changes, database schema changes, new integrations, infrastructure changes
      
      4. **Identify documentation impact**:
         - Which existing docs need updates?
         - What new documentation is needed?
         - Are there breaking changes that need migration guides?

      ### Phase 4: Documentation Updates
      Based on your analysis, create or update documentation:
      
      1. **For code changes and new features**:
         - Update relevant existing documentation files
         - Create new documentation for new features/modules
         - Update API documentation if endpoints changed
         - Update configuration documentation if settings changed
         - Add usage examples for new functionality
      
      2. **For significant/architectural changes, CREATE MERMAID DIAGRAMS**:
         
         **When to create diagrams** (if ANY of these apply):
         - New modules or services added
         - Changes to system architecture
         - New integrations with external systems
         - Database schema changes
         - New design patterns introduced
         - Changes to data flow or processing pipelines
         - New API endpoints or service boundaries
         
         **Types of diagrams to create**:
         
         a) **Architecture Diagram** (for structural changes):
         ```mermaid
         graph TB
             subgraph "Component Name"
                 A[Service A] --> B[Service B]
                 B --> C[Database]
             end
         ```
         
         b) **Sequence Diagram** (for new workflows/processes):
         ```mermaid
         sequenceDiagram
             participant User
             participant API
             participant Service
             User->>API: Request
             API->>Service: Process
             Service-->>API: Response
             API-->>User: Result
         ```
         
         c) **Class/Entity Diagram** (for data model changes):
         ```mermaid
         classDiagram
             class EntityName {
                 +field1: Type
                 +field2: Type
                 +method(): ReturnType
             }
         ```
         
         d) **Flowchart** (for decision logic or processes):
         ```mermaid
         flowchart TD
             A[Start] --> B{Decision}
             B -->|Yes| C[Action 1]
             B -->|No| D[Action 2]
         ```
         
         e) **State Diagram** (for state machine changes):
         ```mermaid
         stateDiagram-v2
             [*] --> State1
             State1 --> State2: Event
             State2 --> [*]
         ```
      
      3. **Documentation file structure**:
         - Place diagrams in appropriate documentation files
         - Create a dedicated `docs/architecture/` folder if it doesn't exist for architectural diagrams
         - Update the main README.md with links to new documentation
         - Ensure all new docs are linked from existing navigation

      ### Phase 5: Branch Creation & Commit
      1. Create a new documentation branch: `docs/{/resource/pushedBy/displayName}-{timestamp}`
      2. Stage all documentation changes
      3. Commit with a descriptive message following this format:
         ```
         docs: Update documentation for {brief description of code changes}
         
         Changes include:
         - {list of documentation updates}
         - {list of new diagrams if any}
         
         Related to commit: {original commit message}
         [AGENT-DOCS]
         ```
      4. Push the branch to the remote repository

      ### Phase 6: Pull Request Creation
      1. Use Azure DevOps CLI commands (`az devops`) to create a pull request:
         ```bash
         az repos pr create \
           --repository {/resource/repository/id} \
           --source-branch docs/{branch-name} \
           --target-branch {/resource/repository/defaultBranch} \
           --title "docs: Documentation updates for recent changes" \
           --description "{comprehensive PR description}"
         ```
      
      2. The PR description MUST include:
         - Summary of code changes that triggered this documentation update
         - List of documentation files created/modified
         - List of diagrams added (if any)
         - Preview of any architectural diagrams in the description

      ## DOCUMENTATION STANDARDS

      ### Writing Style
      - Use clear, concise language
      - Write in present tense for current functionality
      - Include code examples where appropriate
      - Use consistent heading hierarchy (# for title, ## for sections, ### for subsections)

      ### Mermaid Diagram Standards
      - Always include a title comment above the diagram
      - Use descriptive node labels
      - Keep diagrams focused and not overly complex
      - Split complex systems into multiple diagrams
      - Use consistent styling within the project

      ### File Naming Conventions
      - Use lowercase with hyphens: `feature-name.md`
      - Architecture docs: `docs/architecture/{component}-architecture.md`
      - API docs: `docs/api/{endpoint-group}.md`
      - Guides: `docs/guides/{topic}-guide.md`

      ## CRITICAL RULES
      1. **MANDATORY**: Review the entire docs folder BEFORE making any changes
      2. **MANDATORY**: Generate and analyze the git diff to understand all changes
      3. **MANDATORY**: Create mermaid diagrams for ANY significant architectural changes
      4. Always create documentation changes on a new branch, never commit directly to the default branch
      5. Ensure all documentation accurately reflects the code changes
      6. Use clear, professional language in all documentation
      7. Match the existing documentation style and conventions
      8. All commits must end with [AGENT-DOCS]
      9. The pull request must be created using Azure DevOps CLI commands
      10. Include links to new documentation in existing navigation/README files
      11. Diagrams must be in mermaid format for Azure DevOps wiki compatibility

      ## OUTPUT REQUIREMENTS
      Your output must include:
      - `pr_url`: The URL of the created pull request
      - `docs_updated`: List of documentation files that were updated
      - `docs_created`: List of new documentation files created
      - `diagrams_created`: List of mermaid diagrams added (with brief descriptions)
      - `reason`: Explanation if the task could not be completed
            """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        templateProcessor = new TemplateProcessor(objectMapper);
    }

    @Test
    @DisplayName("Should merge ADO instruction template with git.push webhook JSON data and resolve all dynamic fields")
    void shouldMergeInstructionTemplateWithAdoGitPushData() throws Exception {
        // When
        String result = templateProcessor.processTemplate(ADO_INSTRUCTION_TEMPLATE, ADO_GIT_PUSH_JSON);

        // Then
        assertNotNull(result);
        
        // Verify repository URL and ID are resolved
        assertTrue(result.contains("https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code"),
                "Should contain the repository remote URL");
        assertTrue(result.contains("76b3481e-f72a-496a-b7d1-ba015f1628b8"),
                "Should contain the repository ID");
        
        // Verify resourceContainers IDs are resolved
        assertTrue(result.contains("c12d0eb8-e382-443b-9f9c-c52cba5014c2"),
                "Should contain the collection ID");
        assertTrue(result.contains("f844ec47-a9db-4511-8281-8b63f4eaf94e"),
                "Should contain the account ID");
        assertTrue(result.contains("be9b3917-87e6-42a4-a549-2bc06a7a878f"),
                "Should contain the resourceContainers project ID");
        
        // Verify project name and default branch are resolved
        assertTrue(result.contains("qodo-code"),
                "Should contain the project name");
        assertTrue(result.contains("refs/heads/trunk"),
                "Should contain the default branch");
        
        // Verify pushedBy displayName is resolved
        assertTrue(result.contains("David"),
                "Should contain the pushed by display name");
        
        // Verify eventType is resolved
        assertTrue(result.contains("git.push"),
                "Should contain the event type");
        
        // Verify message text is resolved
        assertTrue(result.contains("David pushed updates to qodo-code:trunk."),
                "Should contain the message text");
        
        // Verify detailed message text is resolved
        assertTrue(result.contains("David pushed a commit to qodo-code:trunk."),
                "Should contain the detailed message text");
        
        // Verify no unresolved placeholders remain
        assertFalse(result.contains("{/resource/repository/remoteUrl}"),
                "Should not contain unresolved remoteUrl placeholder");
        assertFalse(result.contains("{/resource/repository/id}"),
                "Should not contain unresolved repository id placeholder");
        assertFalse(result.contains("{/resourceContainers/collection/id}"),
                "Should not contain unresolved collection id placeholder");
        assertFalse(result.contains("{/resourceContainers/account/id}"),
                "Should not contain unresolved account id placeholder");
        assertFalse(result.contains("{/resourceContainers/project/id}"),
                "Should not contain unresolved resourceContainers project id placeholder");
        assertFalse(result.contains("{/resource/repository/project/name}"),
                "Should not contain unresolved project name placeholder");
        assertFalse(result.contains("{/resource/repository/defaultBranch}"),
                "Should not contain unresolved default branch placeholder");
        assertFalse(result.contains("{/resource/pushedBy/displayName}"),
                "Should not contain unresolved pushedBy displayName placeholder");
        assertFalse(result.contains("{/eventType}"),
                "Should not contain unresolved eventType placeholder");
        assertFalse(result.contains("{/message/text}"),
                "Should not contain unresolved message text placeholder");
        assertFalse(result.contains("{/detailedMessage/text}"),
                "Should not contain unresolved detailedMessage text placeholder");
    }

    @Test
    @DisplayName("Should have all variables in ADO_INSTRUCTION_TEMPLATE replaced with no remaining {/...} placeholders")
    void shouldReplaceAllVariablesInAdoInstructionTemplate() throws Exception {
        // When
        String result = templateProcessor.processTemplate(ADO_INSTRUCTION_TEMPLATE, ADO_GIT_PUSH_JSON);

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty");

        // Use regex to find any remaining unresolved {/...} variable placeholders
        java.util.regex.Pattern variablePattern = java.util.regex.Pattern.compile("\\{/[^}]+\\}");
        java.util.regex.Matcher matcher = variablePattern.matcher(result);

        // Collect all unresolved variables for detailed error message
        java.util.List<String> unresolvedVariables = new java.util.ArrayList<>();
        while (matcher.find()) {
            unresolvedVariables.add(matcher.group());
        }

        // Assert no unresolved variables remain
        assertTrue(unresolvedVariables.isEmpty(),
                "All {/VARIABLE} placeholders should be replaced. Found unresolved variables: " + unresolvedVariables);
    }

    @Test
    @DisplayName("Should verify each expected variable in ADO_INSTRUCTION_TEMPLATE is correctly resolved")
    void shouldVerifyEachExpectedVariableIsResolved() throws Exception {
        // Given - List of all expected variables in the template
        java.util.List<String> expectedVariables = java.util.List.of(
                "{/resource/repository/remoteUrl}",
                "{/resource/repository/id}",
                "{/resourceContainers/collection/id}",
                "{/resourceContainers/account/id}",
                "{/resourceContainers/project/id}",
                "{/resource/repository/project/name}",
                "{/resource/repository/defaultBranch}",
                "{/resource/pushedBy/displayName}",
                "{/eventType}",
                "{/message/text}",
                "{/detailedMessage/text}"
        );

        // Verify all expected variables exist in the template
        for (String variable : expectedVariables) {
            assertTrue(ADO_INSTRUCTION_TEMPLATE.contains(variable),
                    "Template should contain variable: " + variable);
        }

        // When
        String result = templateProcessor.processTemplate(ADO_INSTRUCTION_TEMPLATE, ADO_GIT_PUSH_JSON);

        // Then - Verify none of the expected variables remain in the result
        for (String variable : expectedVariables) {
            assertFalse(result.contains(variable),
                    "Variable should be replaced: " + variable);
        }

        // Verify expected resolved values are present
        java.util.Map<String, String> expectedReplacements = java.util.Map.ofEntries(
                java.util.Map.entry("{/resource/repository/remoteUrl}", "https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code"),
                java.util.Map.entry("{/resource/repository/id}", "76b3481e-f72a-496a-b7d1-ba015f1628b8"),
                java.util.Map.entry("{/resourceContainers/collection/id}", "c12d0eb8-e382-443b-9f9c-c52cba5014c2"),
                java.util.Map.entry("{/resourceContainers/account/id}", "f844ec47-a9db-4511-8281-8b63f4eaf94e"),
                java.util.Map.entry("{/resourceContainers/project/id}", "be9b3917-87e6-42a4-a549-2bc06a7a878f"),
                java.util.Map.entry("{/resource/repository/project/name}", "qodo-code"),
                java.util.Map.entry("{/resource/repository/defaultBranch}", "refs/heads/trunk"),
                java.util.Map.entry("{/resource/pushedBy/displayName}", "David"),
                java.util.Map.entry("{/eventType}", "git.push"),
                java.util.Map.entry("{/message/text}", "David pushed updates to qodo-code:trunk."),
                java.util.Map.entry("{/detailedMessage/text}", "David pushed a commit to qodo-code:trunk.")
        );

        for (java.util.Map.Entry<String, String> entry : expectedReplacements.entrySet()) {
            assertTrue(result.contains(entry.getValue()),
                    "Result should contain resolved value '" + entry.getValue() + "' for variable " + entry.getKey());
        }
    }

    @Test
    @DisplayName("Should count and verify total number of variable replacements in ADO_INSTRUCTION_TEMPLATE")
    void shouldCountAndVerifyTotalVariableReplacements() throws Exception {
        // Given - Count variables in original template
        java.util.regex.Pattern variablePattern = java.util.regex.Pattern.compile("\\{/[^}]+\\}");
        java.util.regex.Matcher templateMatcher = variablePattern.matcher(ADO_INSTRUCTION_TEMPLATE);

        java.util.List<String> templateVariables = new java.util.ArrayList<>();
        while (templateMatcher.find()) {
            templateVariables.add(templateMatcher.group());
        }

        // Template should have exactly 13 variable placeholders (some appear multiple times)
        // Unique variables: 11, but {/resource/repository/remoteUrl} and {/resource/repository/defaultBranch} appear twice
        assertTrue(templateVariables.size() >= 11,
                "Template should have at least 11 variable placeholders, found: " + templateVariables.size());

        // When
        String result = templateProcessor.processTemplate(ADO_INSTRUCTION_TEMPLATE, ADO_GIT_PUSH_JSON);

        // Then - Count remaining variables in result (should be 0)
        java.util.regex.Matcher resultMatcher = variablePattern.matcher(result);
        java.util.List<String> remainingVariables = new java.util.ArrayList<>();
        while (resultMatcher.find()) {
            remainingVariables.add(resultMatcher.group());
        }

        assertEquals(0, remainingVariables.size(),
                "All variables should be replaced. Remaining: " + remainingVariables);
    }

    @Test
    @DisplayName("Should extract repository remoteUrl from ADO JSON")
    void shouldExtractRepositoryRemoteUrl() throws Exception {
        // Given
        String template = "Clone repository from: {/resource/repository/remoteUrl}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Clone repository from: https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code", result);
    }

    @Test
    @DisplayName("Should extract repository id from ADO JSON")
    void shouldExtractRepositoryId() throws Exception {
        // Given
        String template = "Repository ID: {/resource/repository/id}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Repository ID: 76b3481e-f72a-496a-b7d1-ba015f1628b8", result);
    }

    @Test
    @DisplayName("Should extract repository name from ADO JSON")
    void shouldExtractRepositoryName() throws Exception {
        // Given
        String template = "Repository: {/resource/repository/name}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Repository: qodo-code", result);
    }

    @Test
    @DisplayName("Should extract project name from ADO JSON")
    void shouldExtractProjectName() throws Exception {
        // Given
        String template = "Project: {/resource/repository/project/name}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Project: qodo-code", result);
    }

    @Test
    @DisplayName("Should extract pushedBy displayName from ADO JSON")
    void shouldExtractPushedByDisplayName() throws Exception {
        // Given
        String template = "Pushed by: {/resource/pushedBy/displayName}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Pushed by: David", result);
    }

    @Test
    @DisplayName("Should extract eventType from ADO JSON")
    void shouldExtractEventType() throws Exception {
        // Given
        String template = "Event: {/eventType}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Event: git.push", result);
    }

    @Test
    @DisplayName("Should extract message text from ADO JSON")
    void shouldExtractMessageText() throws Exception {
        // Given
        String template = "Message: {/message/text}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Message: David pushed updates to qodo-code:trunk.", result);
    }

    @Test
    @DisplayName("Should extract default branch from ADO JSON")
    void shouldExtractDefaultBranch() throws Exception {
        // Given
        String template = "Default branch: {/resource/repository/defaultBranch}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Default branch: refs/heads/trunk", result);
    }

    @Test
    @DisplayName("Should extract commits array as JSON string from ADO JSON")
    void shouldExtractCommitsArray() throws Exception {
        // Given
        String template = "Commits: {/resource/commits}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertTrue(result.contains("33b55f7cb7e7e245323987634f960cf4a6e6bc74"),
                "Should contain the commit ID");
        assertTrue(result.contains("Fixed bug in web.config file"),
                "Should contain the commit comment");
    }

    @Test
    @DisplayName("Should extract first commit details from ADO JSON")
    void shouldExtractFirstCommitDetails() throws Exception {
        // Given
        String template = "Commit ID: {/resource/commits/0/commitId}, Comment: {/resource/commits/0/comment}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Commit ID: 33b55f7cb7e7e245323987634f960cf4a6e6bc74, Comment: Fixed bug in web.config file", result);
    }

    @Test
    @DisplayName("Should extract commit author details from ADO JSON")
    void shouldExtractCommitAuthorDetails() throws Exception {
        // Given
        String template = "Author: {/resource/commits/0/author/name} <{/resource/commits/0/author/email}>";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Author: David <david@example.com>", result);
    }

    @Test
    @DisplayName("Should extract refUpdates branch name from ADO JSON")
    void shouldExtractRefUpdatesBranchName() throws Exception {
        // Given
        String template = "Branch updated: {/resource/refUpdates/0/name}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Branch updated: refs/heads/trunk", result);
    }

    @Test
    @DisplayName("Should preserve placeholder when path not found in ADO JSON")
    void shouldPreservePlaceholderWhenPathNotFound() throws Exception {
        // Given
        String template = "Unknown field: {/nonexistent/path}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Unknown field: {/nonexistent/path}", result,
                "Should preserve the original placeholder when path is not found");
    }

    @Test
    @DisplayName("Should handle multiple placeholders in single template")
    void shouldHandleMultiplePlaceholders() throws Exception {
        // Given
        String template = "Event {/eventType} in project {/resource/repository/project/name} by {/resource/pushedBy/displayName}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Event git.push in project qodo-code by David", result);
    }

    @Test
    @DisplayName("Should extract subscriptionId from ADO JSON")
    void shouldExtractSubscriptionId() throws Exception {
        // Given
        String template = "Subscription: {/subscriptionId}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Subscription: 00000000-0000-0000-0000-000000000000", result);
    }

    @Test
    @DisplayName("Should extract numeric pushId from ADO JSON")
    void shouldExtractNumericPushId() throws Exception {
        // Given
        String template = "Push ID: {/resource/pushId}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Push ID: 14", result);
    }

    @Test
    @DisplayName("Should extract resourceContainers project id from ADO JSON")
    void shouldExtractResourceContainersProjectId() throws Exception {
        // Given
        String template = "Container Project ID: {/resourceContainers/project/id}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Container Project ID: be9b3917-87e6-42a4-a549-2bc06a7a878f", result);
    }

    @Test
    @DisplayName("Should extract resourceContainers collection id from ADO JSON")
    void shouldExtractResourceContainersCollectionId() throws Exception {
        // Given
        String template = "Collection ID: {/resourceContainers/collection/id}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Collection ID: c12d0eb8-e382-443b-9f9c-c52cba5014c2", result);
    }

    @Test
    @DisplayName("Should extract resourceContainers account id from ADO JSON")
    void shouldExtractResourceContainersAccountId() throws Exception {
        // Given
        String template = "Account ID: {/resourceContainers/account/id}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Account ID: f844ec47-a9db-4511-8281-8b63f4eaf94e", result);
    }

    @Test
    @DisplayName("Should extract detailedMessage text from ADO JSON")
    void shouldExtractDetailedMessageText() throws Exception {
        // Given
        String template = "Detailed: {/detailedMessage/text}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertTrue(result.contains("David pushed a commit to qodo-code:trunk."),
                "Should contain the detailed message text");
        assertTrue(result.contains("Fixed bug in web.config file"),
                "Should contain the commit description in detailed message");
    }

    @Test
    @DisplayName("Should extract all resourceContainers IDs in single template")
    void shouldExtractAllResourceContainersIds() throws Exception {
        // Given
        String template = "Collection: {/resourceContainers/collection/id}, Account: {/resourceContainers/account/id}, Project: {/resourceContainers/project/id}";

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertEquals("Collection: c12d0eb8-e382-443b-9f9c-c52cba5014c2, Account: f844ec47-a9db-4511-8281-8b63f4eaf94e, Project: be9b3917-87e6-42a4-a549-2bc06a7a878f", result);
    }

    @Test
    @DisplayName("Should build complete az devops PR command from ADO JSON")
    void shouldBuildCompleteAzDevopsPrCommand() throws Exception {
        // Given
        String template = """
                az repos pr create \\
                  --repository {/resource/repository/id} \\
                  --source-branch {/resource/refUpdates/0/name} \\
                  --target-branch {/resource/repository/defaultBranch} \\
                  --title "PR for commit by {/resource/pushedBy/displayName}" \\
                  --description "{/resource/commits/0/comment}"
                """;

        // When
        String result = templateProcessor.processTemplate(template, ADO_GIT_PUSH_JSON);

        // Then
        assertTrue(result.contains("--repository 76b3481e-f72a-496a-b7d1-ba015f1628b8"));
        assertTrue(result.contains("--source-branch refs/heads/trunk"));
        assertTrue(result.contains("--target-branch refs/heads/trunk"));
        assertTrue(result.contains("--title \"PR for commit by David\""));
        assertTrue(result.contains("--description \"Fixed bug in web.config file\""));
    }

    @Test
    @DisplayName("Test what was passed")
    void shouldPassTheData() throws Exception {
        String data = """
                {"subscriptionId":"00000000-0000-0000-0000-000000000000","notificationId":1,"id":"03c164c2-8912-4d5e-8009-3707d5f83734","eventType":"git.push","publisherId":"tfs","message":{"text":"David pushed updates to qodo-code:trunk.","html":"David pushed updates to qodo-code:trunk.","markdown":"David pushed updates to `qodo-code`:`trunk`."},"detailedMessage":{"text":"David pushed a commit to qodo-code:trunk.\\n - Fixed bug in web.config file 33b55f7c","html":"David pushed a commit to <a href=\\"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code\\">qodo-code</a>:<a href=\\"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code#version=GBtrunk\\">trunk</a>.\\n<ul>\\n<li>Fixed bug in web.config file <a href=\\"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code/commit/33b55f7cb7e7e245323987634f960cf4a6e6bc74\\">33b55f7c</a>\\n</ul>","markdown":"David pushed a commit to [qodo-code](https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code):[trunk](https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code#version=GBtrunk).\\n* Fixed bug in web.config file [33b55f7c](https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code/commit/33b55f7cb7e7e245323987634f960cf4a6e6bc74)"},"resource":{"commits":[{"commitId":"33b55f7cb7e7e245323987634f960cf4a6e6bc74","author":{"name":"David","email":"david@example.com","date":"2015-02-25T19:01:00Z"},"committer":{"name":"David","email":"david@example.com","date":"2015-02-25T19:01:00Z"},"comment":"Fixed bug in web.config file","url":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code/commit/33b55f7cb7e7e245323987634f960cf4a6e6bc74"}],"refUpdates":[{"name":"refs/heads/trunk","oldObjectId":"aad331d8d3b131fa9ae03cf5e53965b51942618a","newObjectId":"33b55f7cb7e7e245323987634f960cf4a6e6bc74"}],"repository":{"id":"76b3481e-f72a-496a-b7d1-ba015f1628b8","name":"qodo-code","url":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_apis/git/repositories/76b3481e-f72a-496a-b7d1-ba015f1628b8","project":{"id":"6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c","name":"qodo-code","url":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_apis/projects/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c","state":"wellFormed","visibility":"unchanged","lastUpdateTime":"0001-01-01T00:00:00"},"defaultBranch":"refs/heads/trunk","remoteUrl":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code"},"pushedBy":{"displayName":"David","id":"00067FFED5C7AF52@Live.com","uniqueName":"david@example.com"},"pushId":14,"date":"2014-05-02T19:17:13.3309587Z","url":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_apis/git/repositories/76b3481e-f72a-496a-b7d1-ba015f1628b8/pushes/14"},"resourceVersion":"1.0","resourceContainers":{"collection":{"id":"c12d0eb8-e382-443b-9f9c-c52cba5014c2"},"account":{"id":"f844ec47-a9db-4511-8281-8b63f4eaf94e"},"project":{"id":"be9b3917-87e6-42a4-a549-2bc06a7a878f"}},"createdDate":"2026-01-23T19:33:29.5724811Z"}
                """;
        String template = """
                You are a documentation agent that analyzes code changes from Azure DevOps push events and updates project documentation accordingly. Your role is to understand the existing documentation structure, analyze code changes, and create comprehensive documentation updates including architectural diagrams when significant changes are detected.
                command-sdk-app  |\s
                command-sdk-app  | ## INPUT CONTEXT
                command-sdk-app  | The following variables contain information about the push event that triggered this workflow:
                command-sdk-app  | - Repository Remote URL: {/resource/repository/remoteUrl}
                command-sdk-app  | - Repository ID: {/resource/repository/id}
                command-sdk-app  | - Collection ID: {/resourceContainers/collection/id}
                command-sdk-app  | - Account ID: {/resourceContainers/account/id}
                command-sdk-app  | - Project ID: {/resourceContainers/project/id}
                command-sdk-app  | - Project Name: {/resource/repository/project/name}
                command-sdk-app  | - Default Branch: {/resource/repository/defaultBranch}
                command-sdk-app  | - Pushed By: {/resource/pushedBy/displayName}
                command-sdk-app  | - Event Type: {/eventType}
                command-sdk-app  | - Commit Message: {/message/text}
                command-sdk-app  | - Detailed Message: {/detailedMessage/text}
                command-sdk-app  |\s
                command-sdk-app  | ## WORKFLOW
                command-sdk-app  |\s
                command-sdk-app  | ### Phase 1: Repository Setup
                command-sdk-app  | 1. Clone the git repository using the remote URL: {/resource/repository/remoteUrl}
                command-sdk-app  | 2. Checkout the branch that was pushed to (extract from the push event details)
                command-sdk-app  | 3. Use `get_project_tree` to understand the complete project structure
                command-sdk-app  |\s
                command-sdk-app  | ### Phase 2: Existing Documentation Analysis (MANDATORY)
                command-sdk-app  | **Before making any changes, you MUST thoroughly review the existing documentation:**
                command-sdk-app  |\s
                command-sdk-app  | 1. **Locate the docs folder**: Search for documentation directories in common locations:
                command-sdk-app  |    - `docs/`
                command-sdk-app  |    - `documentation/`
                command-sdk-app  |    - `doc/`
                command-sdk-app  |    - `.github/`
                command-sdk-app  |    - `wiki/`
                command-sdk-app  |    - Root level markdown files (README.md, CONTRIBUTING.md, etc.)
                command-sdk-app  |\s
                command-sdk-app  | 2. **Read and analyze ALL existing documentation files**:
                command-sdk-app  |    - Use `terminal_execute_command` to list all files: `find docs -type f -name "*.md" 2>/dev/null || find . -maxdepth 2 -name "*.md"`
                command-sdk-app  |    - Read each documentation file to understand:
                command-sdk-app  |      * Current documentation structure and organization
                command-sdk-app  |      * Writing style and formatting conventions used
                command-sdk-app  |      * Existing architectural diagrams (look for mermaid blocks)
                command-sdk-app  |      * Table of contents and navigation patterns
                command-sdk-app  |      * Cross-references between documents
                command-sdk-app  |\s
                command-sdk-app  | 3. **Document your findings**:
                command-sdk-app  |    - Note the documentation hierarchy
                command-sdk-app  |    - Identify the main entry points (README, index files)
                command-sdk-app  |    - Understand how different topics are organized
                command-sdk-app  |    - Note any existing design/architecture documentation
                command-sdk-app  |\s
                command-sdk-app  | ### Phase 3: Code Change Analysis (MANDATORY)
                command-sdk-app  | **Analyze the diff between the pushed branch and the default branch:**
                command-sdk-app  |\s
                command-sdk-app  | 1. **Generate the diff**: Use `terminal_execute_command` to run:
                command-sdk-app  |    ```bash
                command-sdk-app  |    git diff {/resource/repository/defaultBranch}...HEAD --stat
                command-sdk-app  |    git diff {/resource/repository/defaultBranch}...HEAD
                command-sdk-app  |    ```
                command-sdk-app  |\s
                command-sdk-app  | 2. **Categorize the changes**:
                command-sdk-app  |    - **New files added**: List all new files and their purposes
                command-sdk-app  |    - **Modified files**: Identify what was changed and why
                command-sdk-app  |    - **Deleted files**: Note any removed functionality
                command-sdk-app  |    - **Renamed/moved files**: Track file relocations
                command-sdk-app  |\s
                command-sdk-app  | 3. **Assess change significance**:
                command-sdk-app  |    - **Minor changes**: Bug fixes, typo corrections, small refactors
                command-sdk-app  |    - **Moderate changes**: New features, API additions, configuration changes
                command-sdk-app  |    - **Significant/Architectural changes**: New modules, design pattern changes, database schema changes, new integrations, infrastructure changes
                command-sdk-app  |\s
                command-sdk-app  | 4. **Identify documentation impact**:
                command-sdk-app  |    - Which existing docs need updates?
                command-sdk-app  |    - What new documentation is needed?
                command-sdk-app  |    - Are there breaking changes that need migration guides?
                command-sdk-app  |\s
                command-sdk-app  | ### Phase 4: Documentation Updates
                command-sdk-app  | Based on your analysis, create or update documentation:
                command-sdk-app  |\s
                command-sdk-app  | 1. **For code changes and new features**:
                command-sdk-app  |    - Update relevant existing documentation files
                command-sdk-app  |    - Create new documentation for new features/modules
                command-sdk-app  |    - Update API documentation if endpoints changed
                command-sdk-app  |    - Update configuration documentation if settings changed
                command-sdk-app  |    - Add usage examples for new functionality
                command-sdk-app  |\s
                command-sdk-app  | 2. **For significant/architectural changes, CREATE MERMAID DIAGRAMS**:
                command-sdk-app  |   \s
                command-sdk-app  |    **When to create diagrams** (if ANY of these apply):
                command-sdk-app  |    - New modules or services added
                command-sdk-app  |    - Changes to system architecture
                command-sdk-app  |    - New integrations with external systems
                command-sdk-app  |    - Database schema changes
                command-sdk-app  |    - New design patterns introduced
                command-sdk-app  |    - Changes to data flow or processing pipelines
                command-sdk-app  |    - New API endpoints or service boundaries
                command-sdk-app  |   \s
                command-sdk-app  |    **Types of diagrams to create**:
                command-sdk-app  |   \s
                command-sdk-app  |    a) **Architecture Diagram** (for structural changes):
                command-sdk-app  |    ```mermaid
                command-sdk-app  |    graph TB
                command-sdk-app  |        subgraph "Component Name"
                command-sdk-app  |            A[Service A] --> B[Service B]
                command-sdk-app  |            B --> C[Database]
                command-sdk-app  |        end
                command-sdk-app  |    ```
                command-sdk-app  |   \s
                command-sdk-app  |    b) **Sequence Diagram** (for new workflows/processes):
                command-sdk-app  |    ```mermaid
                command-sdk-app  |    sequenceDiagram
                command-sdk-app  |        participant User
                command-sdk-app  |        participant API
                command-sdk-app  |        participant Service
                command-sdk-app  |        User->>API: Request
                command-sdk-app  |        API->>Service: Process
                command-sdk-app  |        Service-->>API: Response
                command-sdk-app  |        API-->>User: Result
                command-sdk-app  |    ```
                command-sdk-app  |   \s
                command-sdk-app  |    c) **Class/Entity Diagram** (for data model changes):
                command-sdk-app  |    ```mermaid
                command-sdk-app  |    classDiagram
                command-sdk-app  |        class EntityName {
                command-sdk-app  |            +field1: Type
                command-sdk-app  |            +field2: Type
                command-sdk-app  |            +method(): ReturnType
                command-sdk-app  |        }
                command-sdk-app  |    ```
                command-sdk-app  |   \s
                command-sdk-app  |    d) **Flowchart** (for decision logic or processes):
                command-sdk-app  |    ```mermaid
                command-sdk-app  |    flowchart TD
                command-sdk-app  |        A[Start] --> B{Decision}
                command-sdk-app  |        B -->|Yes| C[Action 1]
                command-sdk-app  |        B -->|No| D[Action 2]
                command-sdk-app  |    ```
                command-sdk-app  |   \s
                command-sdk-app  |    e) **State Diagram** (for state machine changes):
                command-sdk-app  |    ```mermaid
                command-sdk-app  |    stateDiagram-v2
                command-sdk-app  |        [*] --> State1
                command-sdk-app  |        State1 --> State2: Event
                command-sdk-app  |        State2 --> [*]
                command-sdk-app  |    ```
                command-sdk-app  |\s
                command-sdk-app  | 3. **Documentation file structure**:
                command-sdk-app  |    - Place diagrams in appropriate documentation files
                command-sdk-app  |    - Create a dedicated `docs/architecture/` folder if it doesn't exist for architectural diagrams
                command-sdk-app  |    - Update the main README.md with links to new documentation
                command-sdk-app  |    - Ensure all new docs are linked from existing navigation
                command-sdk-app  |\s
                command-sdk-app  | ### Phase 5: Branch Creation & Commit
                command-sdk-app  | 1. Create a new documentation branch: `docs/{/resource/pushedBy/displayName}-{timestamp}`
                command-sdk-app  | 2. Store this branch name to reference in Phase 6 for variable {branch-name}
                command-sdk-app  | 2. Stage all documentation changes
                command-sdk-app  | 3. Commit with a descriptive message following this format:
                command-sdk-app  |    ```
                command-sdk-app  |    docs: Update documentation for {brief description of code changes}
                command-sdk-app  |   \s
                command-sdk-app  |    Changes include:
                command-sdk-app  |    - {list of documentation updates}
                command-sdk-app  |    - {list of new diagrams if any}
                command-sdk-app  |   \s
                command-sdk-app  |    Related to commit: {original commit message}
                command-sdk-app  |    [AGENT-DOCS]
                command-sdk-app  |    ```
                command-sdk-app  | 4. Push the branch to the remote repository
                command-sdk-app  |\s
                command-sdk-app  | ### Phase 6: Pull Request Creation
                command-sdk-app  | 1. Use Azure DevOps CLI commands (`az devops`) to create a pull request:
                command-sdk-app  |    ```bash
                command-sdk-app  |    az repos pr create \\
                command-sdk-app  |      --repository {/resource/repository/id} \\
                command-sdk-app  |      --source-branch docs/{branch-name} \\
                command-sdk-app  |      --target-branch {/resource/repository/defaultBranch} \\
                command-sdk-app  |      --title "docs: Documentation updates for recent changes" \\
                command-sdk-app  |      --description "{comprehensive PR description}"
                command-sdk-app  |    ```
                command-sdk-app  |\s
                command-sdk-app  | 2. The PR description MUST include:
                command-sdk-app  |    - Summary of code changes that triggered this documentation update
                command-sdk-app  |    - List of documentation files created/modified
                command-sdk-app  |    - List of diagrams added (if any)
                command-sdk-app  |    - Preview of any architectural diagrams in the description
                command-sdk-app  |\s
                command-sdk-app  | ## DOCUMENTATION STANDARDS
                command-sdk-app  |\s
                command-sdk-app  | ### Writing Style
                command-sdk-app  | - Use clear, concise language
                command-sdk-app  | - Write in present tense for current functionality
                command-sdk-app  | - Include code examples where appropriate
                command-sdk-app  | - Use consistent heading hierarchy (# for title, ## for sections, ### for subsections)
                command-sdk-app  |\s
                command-sdk-app  | ### Mermaid Diagram Standards
                command-sdk-app  | - Always include a title comment above the diagram
                command-sdk-app  | - Use descriptive node labels
                command-sdk-app  | - Keep diagrams focused and not overly complex
                command-sdk-app  | - Split complex systems into multiple diagrams
                command-sdk-app  | - Use consistent styling within the project
                command-sdk-app  |\s
                command-sdk-app  | ### File Naming Conventions
                command-sdk-app  | - Use lowercase with hyphens: `feature-name.md`
                command-sdk-app  | - Architecture docs: `docs/architecture/{component}-architecture.md`
                command-sdk-app  | - API docs: `docs/api/{endpoint-group}.md`
                command-sdk-app  | - Guides: `docs/guides/{topic}-guide.md`
                command-sdk-app  |\s
                command-sdk-app  | ## CRITICAL RULES
                command-sdk-app  | 1. **MANDATORY**: Review the entire docs folder BEFORE making any changes
                command-sdk-app  | 2. **MANDATORY**: Generate and analyze the git diff to understand all changes
                command-sdk-app  | 3. **MANDATORY**: Create mermaid diagrams for ANY significant architectural changes
                command-sdk-app  | 4. Always create documentation changes on a new branch, never commit directly to the default branch
                command-sdk-app  | 5. Ensure all documentation accurately reflects the code changes
                command-sdk-app  | 6. Use clear, professional language in all documentation
                command-sdk-app  | 7. Match the existing documentation style and conventions
                command-sdk-app  | 8. All commits must end with [AGENT-DOCS]
                command-sdk-app  | 9. The pull request must be created using Azure DevOps CLI commands
                command-sdk-app  | 10. Include links to new documentation in existing navigation/README files
                command-sdk-app  | 11. Diagrams must be in mermaid format for Azure DevOps wiki compatibility
                command-sdk-app  |\s
                command-sdk-app  | ## OUTPUT REQUIREMENTS
                command-sdk-app  | Your output must include:
                command-sdk-app  | - `pr_url`: The URL of the created pull request
                command-sdk-app  | - `docs_updated`: List of documentation files that were updated
                command-sdk-app  | - `docs_created`: List of new documentation files created
                command-sdk-app  | - `diagrams_created`: List of mermaid diagrams added (with brief descriptions)
                command-sdk-app  | - `reason`: Explanation if the task could not be completed
                """;
        String result = templateProcessor.processTemplate(template, data);

        System.out.println(result);

    }


}
