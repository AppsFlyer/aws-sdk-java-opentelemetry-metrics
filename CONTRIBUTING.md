# Contributing to AWS SDK Java OpenTelemetry Metrics

We welcome contributions to this project! Below are guidelines to help you get started.

## How to Contribute

1. **Fork the repository**:  
   Create your own fork of the repository by clicking the "Fork" button in GitHub.

2. **Create a feature branch**:  
   Clone your fork locally, then create a feature branch for your work:

   ```bash
   git checkout -b feature/my-new-feature
   ```
   
3. **Make your changes**:  
   Implement your changes, ensuring that they follow the project’s coding standards and best practices.

4. **Commit your changes**:  
   Commit your changes to your feature branch:

   ```bash
   git commit -am 'Add some feature'
   ```
   
5. **Push your changes to your fork**:  
   Push your changes to your fork on GitHub:

   ```bash
   git push origin feature/my-new-feature
   ```

6. **Create a Pull Request**:
   Once your changes are ready, open a pull request (PR) from your branch on GitHub.  
   - Ensure that your PR description explains what changes you’ve made and why.
   - Mention any related issues, if applicable.

## Running Tests

Before submitting a PR, make sure all tests pass:

```bash
./mvnw test
```

If you’ve added new features, consider adding appropriate unit tests as well.

## Code of Conduct

Please note that this project is governed by a [Code of Conduct]. By participating, you are expected to uphold this code.

[Code of Conduct]: CODE_OF_CONDUCT.md