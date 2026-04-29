import { motion } from "framer-motion";
import { Shield, Eye, Lock, Database, Globe, Mail } from "lucide-react";

const PrivacyPolicy = () => {
  return (
    <div className="mx-auto max-w-3xl px-4 py-8 md:px-8 md:py-12">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        <div className="mb-8">
          <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-foreground mb-2">Privacy Policy</h1>
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
              <Shield className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Our Commitment to Privacy</h2>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed">
              At R8N, we take your privacy seriously. This Privacy Policy explains how we collect,
              use, and protect your personal information. By using our platform, you agree to the
              practices described in this policy.
            </p>
          </motion.div>

          {/* Information We Collect */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <Database className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Information We Collect</h2>
            </div>
            <div className="space-y-4">
              <div>
                <h3 className="text-sm font-medium text-foreground mb-2">Personal Information</h3>
                <p className="text-sm text-muted-foreground leading-relaxed">
                  We collect information you provide directly, including your name, email address,
                  optional phone number, claimed location jurisdiction, profile information, and any
                  content you create on the platform.
                </p>
              </div>
              <div>
                <h3 className="text-sm font-medium text-foreground mb-2">Session Information</h3>
                <p className="text-sm text-muted-foreground leading-relaxed">
                  We store session begin and end information, and record direct actions such as
                  creating, editing, or deleting opinions and opinion lists, and sending, accepting,
                  or rejecting subscription requests.
                </p>
              </div>
            </div>
          </motion.div>

          {/* How We Use Your Information */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <Eye className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">How We Use Your Information</h2>
            </div>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Provide and maintain the R8N platform</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Process and manage your reviews and lists</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Send you notifications about account activity</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Improve our services and develop new features</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Ensure security and prevent fraud</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Comply with legal obligations</span>
              </li>
            </ul>
          </motion.div>

          {/* Data Protection */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.4 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <Lock className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Data Protection & Security</h2>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed mb-3">
              We implement appropriate technical and organizational measures to protect your personal
              information against unauthorized access, alteration, disclosure, or destruction.
            </p>
            <p className="text-sm text-muted-foreground leading-relaxed mb-3">
              <strong className="text-foreground">Private Communication:</strong> Reviews on R8N are
              private and only visible to users you manually approve. In accordance with German
              privacy standards, this constitutes private correspondence, not public broadcast.
            </p>
            <p className="text-sm text-muted-foreground leading-relaxed">
              However, no method of transmission over the internet is completely secure. While we
              strive to protect your data, we cannot guarantee absolute security.
            </p>
          </motion.div>

          {/* Your Rights */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.5 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <Shield className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Your Rights</h2>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed mb-3">
              Under applicable data protection laws, you have the right to:
            </p>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Access your personal information</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Correct inaccurate information</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Request deletion of your data</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Object to processing of your data</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary mt-1">•</span>
                <span>Data portability</span>
              </li>
            </ul>
          </motion.div>

          {/* Third-Party Services */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.6 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <Globe className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Third-Party Services</h2>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed mb-3">
              We may use third-party services to help operate our platform, including hosting,
              analytics, and authentication providers. These services have access to your
              information only to perform specific tasks on our behalf.
            </p>
            <p className="text-sm text-muted-foreground leading-relaxed mb-3">
              <strong className="text-foreground">Address Lookup:</strong> When you type an address
              while creating a new place or brand, the text you enter is sent to Photon, an
              open-source geocoding service operated by komoot GmbH (Germany), to provide address
              suggestions. Photon uses OpenStreetMap data. We do not store the queries on our side;
              komoot may retain request logs as described in their service.
            </p>
            <p className="text-sm text-muted-foreground leading-relaxed">
              We do not sell your personal information to third parties for their marketing purposes.
            </p>
          </motion.div>

          {/* Contact */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.7 }}
            className="rounded-2xl border border-border bg-card p-6"
          >
            <div className="flex items-start gap-3 mb-4">
              <Mail className="h-5 w-5 text-primary mt-0.5" />
              <h2 className="text-lg font-medium text-foreground">Contact Us</h2>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed">
              If you have any questions about this Privacy Policy or our data practices, please
              contact us at <span className="text-primary">privacy@r8n.com</span>
            </p>
          </motion.div>
        </div>
      </motion.div>
    </div>
  );
};

export default PrivacyPolicy;