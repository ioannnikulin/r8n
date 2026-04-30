import { motion } from "framer-motion";
import { FileText, Shield, AlertCircle, Users } from "lucide-react";

const TermsOfService = () => {
  return (
    <div className="mx-auto max-w-3xl px-4 py-8 md:px-8 md:py-12">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        <div className="mb-8">
          <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-foreground mb-2">Terms of Service</h1>
          <p className="text-sm text-muted-foreground">Last updated: April 2026</p>
        </div>

        <div className="space-y-6">
          {/* Introduction */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.1 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <FileText className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Introduction</h2>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Welcome to R8N. By using our platform, you agree to these Terms of Service.
              Please read them carefully as they govern your use of our services.
            </p>
          </motion.div>

          {/* Acceptance of Terms */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <Shield className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Acceptance of Terms</h2>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed mb-3">
              By accessing or using R8N, you acknowledge that you have read, understood, and agree
              to be bound by these Terms of Service. If you do not agree with any part of these terms,
              you must not use our platform.
            </p>
            <p className="text-sm text-muted-foreground leading-relaxed">
              We reserve the right to modify these terms at any time. Continued use of the platform after
              changes constitutes acceptance of the updated terms.
            </p>
          </motion.div>

          {/* User Responsibilities */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <Users className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">User Responsibilities</h2>
            </div>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Provide accurate and complete information when creating your profile</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Maintain the security of your account credentials</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Use the platform in accordance with applicable laws and regulations</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Respect the privacy and rights of other users</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Not attempt to circumvent any security measures or access restrictions</span>
              </li>
            </ul>
          </motion.div>

          {/* Content Guidelines */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.4 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <AlertCircle className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Content Guidelines</h2>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed mb-3">
              Users are responsible for all content they post on R8N, including reviews, comments, and
              any other materials. You agree not to post content that:
            </p>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Is defamatory, libelous, or invasive of privacy</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Contains hate speech, discrimination, or harassment</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Infringes on intellectual property rights</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Is fraudulent, deceptive, or misleading</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Violates any applicable laws or regulations</span>
              </li>
            </ul>
          </motion.div>

          {/* Limitation of Liability */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.5 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <AlertCircle className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Limitation of Liability</h2>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed">
              R8N is provided on an "as is" and "as available" basis. We make no representations or
              warranties of any kind, express or implied, regarding the operation or availability of
              the platform. In no event shall we be liable for any indirect, incidental, special,
              consequential, or punitive damages arising from your use of the platform.
            </p>
          </motion.div>

          {/* Contact */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.6 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <h2 className="text-lg font-medium text-foreground mb-3">Contact Us</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              If you have any questions about these Terms of Service, please contact us at
              <span className="text-primary"> legal@r8n.com</span>
            </p>
          </motion.div>
        </div>
      </motion.div>
    </div>
  );
};

export default TermsOfService;